import json
import re
from pathlib import Path

from openai import OpenAI

from ..RestoreTool import RestoreToolOne
from ..SelectTool import SelectToolOne

# 按 url 缓存生成的摘要。
finaldata = {}

# 数据目录：项目根目录下的 data。
DATA_DIR = Path(__file__).resolve().parents[2] / "data"

PROMPT_FILE = DATA_DIR / "summary(en).json"

# 正文长度档位，与提示词 content_length_rule 对齐。
LEN_TIERS = [
    (500, 1000, 2000),      # text < 500  -> content 1000~2000
    (2000, 2000, 4000),     # 500~2000    -> content 2000~4000
    (10**9, 4000, 10000),   # >2000       -> content 4000~10000
]


def load_jsonl(path):
    """逐行读取 JSONL 文件并返回记录列表，文件不存在时返回空列表。"""
    records = []
    if not Path(path).exists():
        return records
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            records.append(json.loads(line))
    return records


def _expected_len_range(text: str):
    """根据输入 text 字符数返回期望的 content 长度区间。"""
    n = len(text or "")
    for limit, lo, hi in LEN_TIERS:
        if n < limit:
            return lo, hi
    return 4000, 10000


def _strip_fence(s: str) -> str:
    """去掉模型可能输出的 ```json ... ``` 围栏。"""
    s = s.strip()
    s = re.sub(r"^```(?:json)?\s*", "", s)
    s = re.sub(r"\s*```$", "", s)
    return s


def _extract_json_obj(s: str) -> str:
    """截取第一个 { 到最后一个 } 之间的内容。"""
    start, end = s.find("{"), s.rfind("}")
    if start == -1 or end == -1:
        return ""
    return s[start:end + 1]


def _validate(result: dict, url: str, text: str) -> tuple[bool, str]:
    """校验模型输出是否符合 title/tags/content 结构。"""
    if not isinstance(result, dict):
        return False, "顶层不是 JSON 对象"

    if set(result.keys()) != {"title", "tags", "content"}:
        return False, f"字段不符，实际为 {list(result.keys())}，应为 title/tags/content"

    title = result.get("title")
    tags = result.get("tags")
    content = result.get("content")

    if not isinstance(title, str) or not title.strip():
        return False, "title 为空或非字符串"
    if len(title) > 20:
        return False, f"title 超过 20 字（当前 {len(title)}）"

    if not isinstance(tags, str):
        return False, f"tags 应为字符串，实际为 {type(tags).__name__}"
    if tags.strip():
        tag_list = tags.split()
        if len(tag_list) > 5:
            return False, f"tags 超过 5 个（当前 {len(tag_list)}）"
        for t in tag_list:
            if not t.startswith("#"):
                return False, f"标签缺少 # 前缀：{t}"
            if len(t) <= 1:
                return False, f"标签为空：{t}"

    if not isinstance(content, str) or not content.strip():
        return False, "content 为空或非字符串"

    # 结尾必须是"项目原帖地址：[项目URL](url)"。
    tail = content.rstrip().splitlines()[-1] if content.strip() else ""
    if not tail.startswith("项目原帖地址："):
        return False, f"content 结尾格式不符：{tail!r}"
    if "]( " in tail:
        return False, f"content 结尾格式不符：{tail!r}"
    if f"({url})" not in tail:
        return False, f"content 结尾未包含输入 url：{tail!r}"

    # 长度档位只做宽松告警，不直接判死。
    lo, hi = _expected_len_range(text)
    if not (lo <= len(content) <= hi):
        return True, f"content 长度 {len(content)} 超出建议区间 {lo}~{hi}（仅告警）"

    return True, "ok"


def mark_by_uri(topic, url):
    """把 NotUsed 文件中 uri 匹配且已筛选的记录移动到 Used 文件。"""
    path1 = DATA_DIR / f"SearchFor{topic}NotUsed.jsonl"
    path2 = DATA_DIR / f"SearchFor{topic}Used.jsonl"
    try:
        data1 = load_jsonl(path1)
        moved = None
        remaining = []
        for d in data1:
            if d.get("uri") == url and d.get("see") is True:
                moved = d
            else:
                remaining.append(d)

        if moved is None:
            return False, f"未找到 uri={url} 且 see=True 的记录"

        moved["use"] = True
        SelectToolOne.keep_jsonl(path1, remaining)
        with open(path2, "a", encoding="utf-8") as f:
            f.write(json.dumps(moved, ensure_ascii=False) + "\n")
        return True, "ok"
    except Exception as e:
        return False, str(e)


def summarize_tool_one(url, text=None, max_retry=2, topic=None):
    """为指定 url 生成中文摘要，写入本地仓库并持久化到 finaldata.jsonl。"""
    print("正在调用方法summarize_tool_one")

    if text is None:
        sentences = RestoreToolOne.get_de_markdown_tool_one_data(url)
        if not sentences:
            print("方法summarize_tool_one调用失败")
            return {
                "status": "error",
                "stdout": None,
                "stderr": f"没有 url={url} 的句子，请先调用 de_markdown_tool_one",
            }
        text = "\n".join(sentences) if isinstance(sentences, list) else str(sentences)

    with open(PROMPT_FILE, "r", encoding="utf-8") as f:
        summary = json.load(f)

    client = OpenAI(
        api_key="yourAPIkey",
        base_url="https://api.deepseek.com",
    )

    system_prompt = json.dumps(summary, ensure_ascii=False)
    user_prompt = json.dumps({"url": url, "text": text}, ensure_ascii=False)

    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": user_prompt},
    ]

    errmsg = []

    for attempt in range(max_retry + 1):
        try:
            response = client.chat.completions.create(
                model="deepseek-chat",
                messages=messages,
                response_format={"type": "json_object"},
                max_tokens=8000,
            )
        except Exception as e:
            last_err = f"API 调用异常：{e}"
            print(f"第 {attempt + 1} 次调用失败：{last_err}")
            errmsg.append(f"第 {attempt + 1} 次调用失败：{last_err}")
            continue

        content = response.choices[0].message.content if response.choices else None

        if not content or not content.strip():
            last_err = "返回内容为空"
            print(f"第 {attempt + 1} 次：{last_err}，重试…")
            errmsg.append(f"第 {attempt + 1} 次：{last_err}，重试…")
            continue

        cleaned = _extract_json_obj(_strip_fence(content))
        if not cleaned:
            last_err = "未找到 JSON 对象"
            print(f"第 {attempt + 1} 次：{last_err}，原始内容：{content!r}")
            errmsg.append(f"第 {attempt + 1} 次：{last_err}，原始内容：{content!r}")
            continue

        try:
            result = json.loads(cleaned)
        except json.JSONDecodeError as e:
            last_err = f"JSON 解析失败：{e}"
            print(f"第 {attempt + 1} 次：{last_err}，原始内容：{content!r}")
            errmsg.append(f"第 {attempt + 1} 次：{last_err}，原始内容：{content!r}")
            continue

        ok, msg = _validate(result, url, text)
        if not ok:
            last_err = f"校验失败：{msg}"
            print(f"第 {attempt + 1} 次：{last_err}，重试…")
            errmsg.append(f"第 {attempt + 1} 次：{last_err}，重试…")
            continue
        if msg != "ok":
            print(f"警告：{msg}")

        print("方法summarize_tool_one调用结束")
        finaldata[url] = result
        client.close()

        RestoreToolOne.add_data("summarize_tool_one", finaldata)

        data2 = {"url": url, **result}
        with open(DATA_DIR / "finaldata.jsonl", "a", encoding="utf-8") as f:
            f.write(json.dumps(data2, ensure_ascii=False) + "\n")

        print(finaldata)
        return {
            "status": "success",
            "stdout": {
                "title": result["title"],
                "tags": result["tags"],
                "content": {
                    "length": len(result["content"]),
                    "first200characters": result["content"][:200],
                },
            },
            "stderr": errmsg,
        }

    print("方法aisummarize1调用结束")
    client.close()
    return {
        "status": "error",
        "stdout": None,
        "stderr": errmsg,
    }
