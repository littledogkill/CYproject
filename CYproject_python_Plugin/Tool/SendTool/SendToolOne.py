import requests

from ..RestoreTool import RestoreToolOne
from ..SummarizeTool import SummarizeToolOne


def send_tool_one(url, token, topic, title1=None, tags1=None, content=None):
    """把 url 对应的标题、标签、正文发布为工具内帖子。"""
    # 未显式传入时，从共享仓库按 url 取回摘要数据。
    if title1 is None or tags1 is None or content is None:
        data = RestoreToolOne.get_summarize_tool_one_data(url)
        if not data:
            return {
                "status": "error",
                "stdout": None,
                "stderr": f"没有 url={url} 的摘要数据，请先调用 aisummarize1",
            }
        title1 = title1 or data.get("title")
        tags1 = tags1 or data.get("tags")
        content = content or data.get("content")

        if isinstance(tags1, str):
            # "#AI设计 #UI-UX #开源" -> ["AI设计", "UI-UX", "开源"]
            tags1 = [t.strip().lstrip("#") for t in tags1.split() if t.strip()]
        elif tags1 is None:
            tags1 = []

    post_url = "http://127.0.0.1:8080/posts"
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    body = {
        "title": title1,
        "category": "work",
        "tags": tags1,
        "content": content,
    }

    try:
        resp = requests.post(post_url, json=body, headers=headers, timeout=10)
    except requests.RequestException as e:
        print(f"请求异常：{e}")
        return {
            "status": "error",
            "stdout": None,
            "stderr": "service_error",
        }

    if resp.status_code == 401:
        print("need_token")
        return {
            "status": "error",
            "stdout": None,
            "stderr": "need_token",
        }

    try:
        data = resp.json()
    except ValueError:
        print(f"响应非 JSON，status={resp.status_code}，text={resp.text[:200]}")
        return {
            "status": "error",
            "stdout": None,
            "stderr": "service_error",
        }

    code = data.get("code")
    if code == 0:
        # 把该 url 对应的记录从 NotUsed 移到 Used。
        ok, message = SummarizeToolOne.mark_by_uri(topic, url)
        if not ok:
            print(f"警告：未能在 NotUsed 文件中移动 uri={url}")
            return {
                "status": "error",
                "stdout": "None",
                "stderr": f"记录存储错误：{message}",
            }
        print("success")
        return {
            "status": "success",
            "stdout": "success",
            "stderr": None,
        }
    if code == 401:
        print("need_token")
        return {
            "status": "error",
            "stdout": None,
            "stderr": "need_token",
        }

    print(f"服务端返回异常 code={code}，body={data}")
    return {
        "status": "error",
        "stdout": None,
        "stderr": "service_error",
    }
