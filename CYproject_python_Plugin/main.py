import inspect
import json

from openai import OpenAI

from SchemasOne import TOOL_REGISTRY, TOOLS

client = OpenAI(
    api_key="yourAPIkey",
    base_url="https://api.deepseek.com",
)


def call_tool(fn, args: dict):
    """按函数签名过滤参数，避免无参工具被塞入多余参数。"""
    sig = inspect.signature(fn)
    accepted = {k: v for k, v in args.items() if k in sig.parameters}
    return fn(**accepted)


def run(user_input: str, max_turns: int = 1000):
    messages = [
        {"role": "system", "content":
            "你是一个会调用工具的助手。任何需要与用户交流（提问、澄清、请求确认结束）"
            "都必须调用 ask_user 工具，不要用普通文本向用户发问。"
            "只有当你百分百确定任务完成、且用户通过 ask_user(confirm_finish=True) 明确同意后，"
            "才可以不再调用工具而直接给出最终回答。"},
        {"role": "user", "content": user_input},
    ]

    for _ in range(max_turns):
        resp = client.chat.completions.create(
            model="deepseek-chat", messages=messages, tools=TOOLS,
        )
        msg = resp.choices[0].message
        messages.append(msg)

        if not msg.tool_calls:
            # 在"交互走工具"的约定下，这里才是真正的最终回答
            print("最终回答：", msg.content)
            return msg.content

        user_agreed_finish = False
        for call in msg.tool_calls:
            name = call.function.name
            args = json.loads(call.function.arguments or "{}")
            fn = TOOL_REGISTRY.get(name)

            if not fn:
                result = {"status": "error", "stdout": None, "stderr": f"未知工具：{name}"}
            else:
                try:
                    result = call_tool(fn, args)
                except Exception as e:
                    result = {"status": "error", "stdout": None, "stderr": f"工具执行异常：{e}"}

            # 交互工具：把用户答复回填，并判断是否同意结束
            if name == "ask_user":
                reply = str(result.get("user_reply", "")).strip().lower()
                if result.get("confirm_finish") and reply in ("y", "yes", "是", "好", "同意", "结束"):
                    user_agreed_finish = True

            messages.append({
                "role": "tool",
                "tool_call_id": call.id,
                "content": json.dumps(result, ensure_ascii=False),
            })

        # 用户已明确同意结束 → 让模型收尾一次
        if user_agreed_finish:
            messages.append({
                "role": "user",
                "content": "用户已确认任务完成，可以结束。请给出最终回答。",
            })

    return None


if __name__ == "__main__":
    run("hello")
