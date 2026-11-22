# 进程内共享数据仓库：各工具通过 add_data/get_* 在这里交换中间结果。
alldata = {}


def add_data(toolname, data):
    """按工具名保存一份共享数据。"""
    alldata[toolname] = data


def get_select_tool_one_data(n):
    """读取 select_tool_one 结果列表中的第 n 项。"""
    data = alldata.get("select_tool_one", [])
    return data[n] if 0 <= n < len(data) else None


def get_search_tool_two_data(url):
    """按 url 读取 search_tool_two 抓取的 README 原文。"""
    return alldata.get("search_tool_two", {}).get(url)


def get_de_markdown_tool_one_data(url):
    """按 url 读取 de_markdown_tool_one 提取的句子列表。"""
    return alldata.get("de_markdown_tool_one", {}).get(url)


def get_summarize_tool_one_data(url):
    """按 url 读取 summarize_tool_one 生成的摘要数据。"""
    return alldata.get("summarize_tool_one", {}).get(url)
