import re
import base64

from ..RestoreTool import RestoreToolOne
from ..SearchTool import SearchToolOne

# 按 url 缓存抓取到的 README 原文。
finaldata = {}

# 匹配 https://github.com/{owner}/{repo}，忽略大小写与末尾斜杠、.git。
GITHUB_RE = re.compile(
    r"https?://github\.com/(?P<owner>[^/]+)/(?P<repo>[^/#?]+)",
    re.IGNORECASE,
)


def parse_github_url(url: str) -> tuple[str, str]:
    """解析 GitHub URL，返回 (owner, repo)；解析失败抛 ValueError。"""
    m = GITHUB_RE.search(url)
    if not m:
        raise ValueError(f"无法解析 GitHub URL: {url}")
    owner = m.group("owner")
    repo = m.group("repo")
    if repo.endswith(".git"):
        repo = repo[:-4]
    return owner, repo


def search_tool_two(url: str):
    """获取指定 GitHub 仓库的 README 原文，并存入共享数据仓库。"""
    print("正在调用方法search_tool_two")

    try:
        owner, repo = parse_github_url(url)
    except ValueError as e:
        print(e)
        print("方法search_tool_two调用结束")
        return {
            "status": "error",
            "stdout": None,
            "stderr": str(e),
        }

    api_url = f"https://api.github.com/repos/{owner}/{repo}/readme"

    with SearchToolOne.create_http_client() as client:
        resp = client.get(api_url)

    if resp.status_code != 200:
        print(f"获取失败，状态码：{resp.status_code}")
        print(resp.text)
        print("方法search_tool_two调用结束")
        return {
            "status": "error",
            "stdout": None,
            "stderr": f"获取失败，状态码：{resp.status_code}",
        }

    data = resp.json()

    # 大文件场景：GitHub 对超过 1MB 的 README 不返回 content，只给 download_url。
    if data.get("encoding") == "none" or "content" not in data:
        raw_url = data.get("download_url")
        if not raw_url:
            print("README 内容不可用（无 content 且无 download_url）")
            print("方法search_tool_two调用结束")
            return {
                "status": "error",
                "stdout": None,
                "stderr": "README 内容不可用（无 content 且无 download_url）",
            }
        with SearchToolOne.create_http_client() as raw_client:
            raw_resp = raw_client.get(raw_url)
        readme_content = raw_resp.text
    else:
        # 默认 Base64 编码，解码时用 replace 兜底非 UTF-8 字符。
        readme_content = base64.b64decode(data["content"]).decode(
            "utf-8", errors="replace"
        )

    finaldata[url] = readme_content
    RestoreToolOne.add_data("search_tool_two", finaldata)

    print(readme_content[:200])
    print("方法search_tool_two调用结束")
    return {
        "status": "success",
        "stdout": "success",
        "stderr": None,
    }
