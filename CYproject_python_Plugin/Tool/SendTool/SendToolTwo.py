import requests


def send_tool_two():
    """登录并获取 token，供 send_tool_one 使用。"""
    useremail = "CCYY@qq.com"
    password = "passwordCCYY"
    url = "http://127.0.0.1:8080/begain/login"
    body = {"email": useremail, "password": password}

    try:
        resp = requests.post(url, json=body, timeout=10)
    except requests.RequestException as e:
        print(f"登录请求失败：{e}")
        return {
            "status": "error",
            "stdout": None,
            "stderr": f"登录请求失败：{e}",
        }

    if resp.status_code != 200:
        msg = f"登录失败，HTTP {resp.status_code}：{resp.text[:200]}"
        print(msg)
        return {
            "status": "error",
            "stdout": None,
            "stderr": msg,
        }

    try:
        data = resp.json()
    except ValueError:
        msg = f"登录响应非 JSON：{resp.text[:200]}"
        print(msg)
        return {
            "status": "error",
            "stdout": None,
            "stderr": msg,
        }

    token = data.get("data", {}).get("token")
    if not token:
        msg = f"登录响应中缺少 token：{data}"
        print(msg)
        return {
            "status": "error",
            "stdout": None,
            "stderr": msg,
        }

    print("success")
    return {
        "status": "success",
        "stdout": token,
        "stderr": None,
    }
