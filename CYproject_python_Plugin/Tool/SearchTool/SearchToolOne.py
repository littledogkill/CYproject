import json
import os
import ssl
from pathlib import Path

import httpx
import truststore

# 数据目录：项目根目录下的 data，与脚本所在层级解耦。
DATA_DIR = Path(__file__).resolve().parents[2] / "data"
DETAIL_PATH = DATA_DIR / "detail.jsonl"

GITHUB_API = "https://api.github.com/search/repositories"
HEADERS = {
    "Accept": "application/vnd.github+json",
    "User-Agent": "github-crawler/1.0",
}


def create_http_client() -> httpx.Client:
    """创建使用本机信任库的 HTTP 客户端。"""
    ctx = truststore.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
    return httpx.Client(verify=ctx, timeout=30)


def repo_key(url: str) -> str:
    """从仓库 URL 中提取 owner/repo 作为去重键。"""
    return "/".join(url.rstrip("/").rsplit("/", 2)[-2:])


def load_existing_keys(path: Path) -> set[str]:
    """读取 JSONL 文件中已有的 owner/repo 键集合。"""
    keys: set[str] = set()
    if not path.exists():
        return keys
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            obj = json.loads(line)
            uri = obj.get("uri")
            if uri:
                keys.add(repo_key(uri))
    return keys


def remove_topic_from_detail(topic: str) -> int:
    """从 detail.jsonl 中删除指定 topic 的旧记录，并返回其 number 之和。"""
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    if not DETAIL_PATH.exists():
        DETAIL_PATH.touch()
        return 0

    removed_number = 0
    tmp_path = DETAIL_PATH.with_suffix(".jsonl.tmp")

    with DETAIL_PATH.open("r", encoding="utf-8") as f, \
            tmp_path.open("w", encoding="utf-8") as out:
        for line in f:
            line = line.strip()
            if not line:
                continue
            obj = json.loads(line)
            if obj.get("search") == topic:
                removed_number += int(obj.get("number", 0))
                continue
            out.write(json.dumps(obj, ensure_ascii=False) + "\n")

    os.replace(tmp_path, DETAIL_PATH)
    return removed_number


def search_tool_one(topic: str, number1: int, pagenumber: int):
    """按主题搜索 GitHub 仓库，去重后写入本地 JSONL，供后续筛选使用。"""
    print("正在调用方法search_tool_one")

    params = {
        "q": f"{topic} in:name,description,topics",
        "sort": "stars",
        "order": "desc",
        "per_page": number1,
        "page": pagenumber,
    }

    with create_http_client() as client:
        resp = client.get(GITHUB_API, params=params, headers=HEADERS)

    print("状态码:", resp.status_code)
    if resp.status_code != 200:
        print("请求失败:", resp.text)
        print("方法search1调用结束")
        return {
            "status": "error",
            "stdout": None,
            "stderr": resp.text,
        }

    # 删除 detail.jsonl 中该 topic 的旧记录，避免统计重复。
    old_number = remove_topic_from_detail(topic)

    # 汇总未使用/已使用两个文件里已有的仓库键，用于去重。
    search_path = DATA_DIR / f"SearchFor{topic}NotUsed.jsonl"
    used_path = DATA_DIR / f"SearchFor{topic}Used.jsonl"
    seen_keys = load_existing_keys(search_path)
    seen_keys.update(load_existing_keys(used_path))

    new_count = 0
    items = resp.json().get("items", [])
    with search_path.open("a", encoding="utf-8") as f:
        for item in items:
            key = repo_key(item["html_url"])
            if key in seen_keys:
                continue
            record = {
                "see": False,
                "uri": item["html_url"],
                "introduce": item.get("description"),
                "language": item.get("language"),
                "stars": item.get("stargazers_count"),
                "updated": item.get("pushed_at"),
            }
            f.write(json.dumps(record, ensure_ascii=False) + "\n")
            seen_keys.add(key)
            new_count += 1

    detail_record = {"search": topic, "number": old_number + new_count}
    with DETAIL_PATH.open("a", encoding="utf-8") as f:
        f.write(json.dumps(detail_record, ensure_ascii=False) + "\n")

    print(f"新增 {new_count} 条，累计 number={detail_record['number']}")
    print("方法search_tool_one调用结束")
    return {
        "status": "success",
        "stdout": "数据已经存入",
        "stderr": None,
    }
