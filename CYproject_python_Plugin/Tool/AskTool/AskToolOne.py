def ask_tool_one(ask):
    message = input(ask+"\n")
    return {
        "status": "success",
        "stdout": message,
        "stderr": None,
    }