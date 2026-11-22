import json
from pathlib import Path

from openai import OpenAI

from ..RestoreTool import RestoreToolOne
from LongTextDeal.textteaser import TextTeaser

# 数据目录：项目根目录下的 data。
DATA_DIR = Path(__file__).resolve().parents[2] / "data"

# 默认筛选指标（深度/广度/流行度/新鲜度）。
idea1 = {
    "depth": "medium",
    "breadth": "high",
    "popularity": "very high",
    "recency": "very high",
}

# 进程内累积所有被选中的 url。
finaldata = []


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


def keep_jsonl(path, data):
    """把记录列表写回 JSONL 文件。"""
    with open(path, "w", encoding="utf-8") as f:
        for d in data:
            f.write(json.dumps(d, ensure_ascii=False) + "\n")


def select_tool_one(topic, n, target_idea=None):
    """按指标从 NotUsed 文件中筛选前 n 条 see=False 的项目，返回 url 列表。"""
    print("正在调用方法select_tool_one")

    # target_idea 由工具 schema 以 JSON 字符串传入，这里统一转成 dict。
    if isinstance(target_idea, str):
        try:
            target_idea = json.loads(target_idea)
        except json.JSONDecodeError:
            target_idea = None
    if not isinstance(target_idea, dict):
        target_idea = idea1

    global finaldata

    data1 = load_jsonl(DATA_DIR / f"SearchFor{topic}NotUsed.jsonl")
    if not data1:
        print(f"警告：{DATA_DIR / f'SearchFor{topic}NotUsed.jsonl'} 为空或不存在，没有可筛选的数据")
        return {
            "status": "error",
            "stdout": None,
            "stderr": f"警告：{DATA_DIR / f'SearchFor{topic}NotUsed.jsonl'} 为空或不存在，没有可筛选的数据",
        }

    data2 = [d for d in data1 if not d.get("see")]
    data = data2[:n]

    # 对超长 introduce 先用 TextTeaser 压缩，避免请求过大。
    for d in data:
        if isinstance(d.get("introduce"), str) and len(d["introduce"]) >= 8000:
            little_title = topic
            content = d.get("introduce")
            tt = TextTeaser()
            sentences = tt.summarize(little_title, content)
            text = ""
            for s in sentences:
                text += s
            d["introduce"] = text

    if not data:
        return {
            "status": "error",
            "stdout": None,
            "stderr": f"未查询到see参数为False的记录，或者你的n过大，see为False的总记录数{len(data2)}，你要求的记录个数{n}",
        }

    with open(DATA_DIR / "idea(en).json", "r", encoding="utf-8") as f:
        idea = json.load(f)

    client = OpenAI(
        api_key="yourAPIkey",
        base_url="https://api.deepseek.com",
    )

    system_prompt = json.dumps({
        "you_need_do": (
            "Based on the depth, breadth, popularity, and freshness levels the user "
            "passes in for the desired AI or agent projects, together with the project list, "
            "use the given evaluation criterion 'idea' to filter out the matching projects, "
            "and return the project URLs in JSON format as follows: "
            '{"item": ["uri1", "uri2", ...]}. '
            "You should have a return rate of at least 40%, but do not change the "
            "filtering criteria just to meet that rate. If this round of filtering does "
            "not go well, we have a corresponding handling mechanism for it; you only "
            'need to return {"item": ["error", "the reason you could not complete the task"]}.'
        ),
        "idea": idea,
    }, ensure_ascii=False)

    user_prompt = json.dumps({
        "idea": target_idea,
        "data": data,
    }, ensure_ascii=False)

    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": user_prompt},
    ]

    response = client.chat.completions.create(
        model="deepseek-chat",
        messages=messages,
        response_format={"type": "json_object"},
        max_tokens=4000,
    )

    content = response.choices[0].message.content

    if not content or not content.strip():
        print("返回内容为空，建议重试或修改提示词")
        print("方法select_tool_one调用结束")
        client.close()
        return {
            "status": "error",
            "stdout": None,
            "stderr": "返回内容为空，建议重试或修改提示词",
        }

    try:
        result = json.loads(content)
    except json.JSONDecodeError as e:
        print("JSON 解析失败：", e)
        print("原始内容：", repr(content))
        print("方法select_tool_one调用结束")
        client.close()
        return {
            "status": "error",
            "stdout": None,
            "stderr": "JSON 解析失败",
        }

    items = result.get("item")
    if not isinstance(items, list):
        print("返回结构不符合预期：", result)
        print("方法select_tool_one调用结束")
        client.close()
        return {
            "status": "error",
            "stdout": None,
            "stderr": "返回结构不符合预期",
        }

    if items and items[0] == "error":
        reason = items[1] if len(items) > 1 else "未知原因"
        print(f"筛选AI有返回异常，{reason}")
        print("方法select_tool_one调用结束")
        client.close()
        return {
            "status": "error",
            "stdout": None,
            "stderr": f"筛选AI有返回异常，{reason}",
        }

    # 只保留确实存在于本轮输入数据中的 uri。
    valid_uris = {d.get("uri") for d in data if d.get("uri")}
    filtered = [u for u in items if isinstance(u, str) and u in valid_uris]

    # 本轮处理过的记录全部标记为 see=True，避免后续重复筛选。
    for d in data1:
        if any(x.get("uri") == d.get("uri") for x in data):
            d["see"] = True

    print(f"模型返回 {len(items)} 条，校验后有效 {len(filtered)} 条")
    print(items)
    keep_jsonl(DATA_DIR / f"SearchFor{topic}NotUsed.jsonl", data1)
    client.close()

    # 累积到全局列表并写入共享数据仓库。
    final1 = list(dict.fromkeys(finaldata + filtered))
    finaldata = final1
    RestoreToolOne.add_data("select_tool_one", final1)

    print("方法select_tool_one调用结束")
    return {
        "status": "success",
        "stdout": filtered,
        "stderr": None,
    }
