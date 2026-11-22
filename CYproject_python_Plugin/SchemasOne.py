from pydantic import BaseModel, Field
from openai import pydantic_function_tool

from Tool.SearchTool import SearchToolOne, SearchToolTwo
from Tool.SelectTool import SelectToolOne
from Tool.DeMarkdownTool import DeMarkdownToolOne
from Tool.SummarizeTool import SummarizeToolOne
from Tool.SendTool import SendToolOne, SendToolTwo
from Tool.GetMessageTool import GetMessageToolOne
from Tool.ResetTool import ResetToolOne
from Tool.AskTool import AskToolOne

class ask_tool_one_params(BaseModel):
    """这个为你和用户之间提供交互工具，你发送信息给用户，用户返回信息给你
    当你对某一步骤有疑问时，可以调用，具体如下：
    你不知道用户想要的输出时；你发现你解决不了的问题时；还有其他，只要你觉得有疑问的地方，都应该调用这个工具
    """
    ask: str = Field(description= "你输入给用户的内容")

class reset_tool_one_params(BaseModel):
    """把某主题下所有记录的 see 置回 False，使已筛选数据重新变为"未使用"。
    仅当该主题已无未使用数据（get_message_tool_one 返回 not_used=0）且仍需继续筛选时调用。
    topic 只能是 "AI" 或 "Agent"。"""
    topic: str = Field(description='主题，只能是 "AI" 或 "Agent"')


class get_message_tool_one_params(BaseModel):
    """查询某主题的搜索数据统计：total（总记录数）、not_used（see=False 的未筛选数）、
    selected（see=True 的已筛选数）。用于判断是否还有未使用数据、是否需要 reset。
    不返回具体记录内容。topic 只能是 "AI" 或 "Agent"。"""
    topic: str = Field(description='主题，只能是 "AI" 或 "Agent"')


class summarize_tool_one_params(BaseModel):
    """为指定 url 的文章生成中文摘要（title、tags、content），结果按 url 存入本地仓库，
    不直接返回全文。前置条件：该 url 已调用 de_markdown_tool_one 提取句子。"""
    url: str = Field(description="原文链接，同时作为摘要存储的键")
    topic: str = Field(description='主题，只能是 "AI" 或 "Agent"')


class de_markdown_tool_one_params(BaseModel):
    """清理文章 Markdown 并按句提取，句子列表按 url 存入本地仓库。
    前置条件：该 url 已调用 search_tool_two 取回 README 正文。
    只返回提取的总字符数和前 200 字符预览，不返回全文。"""
    url: str = Field(description="GitHub 仓库链接，作为正文与句子列表的存储键")


class send_tool_one_params(BaseModel):
    """把已摘要的 url 对应的标题、标签、正文发布为工具内帖子。
    前置条件：该 url 已调用 summarize_tool_one。需要有效 token（来自 send_tool_two）。"""
    url: str = Field(description="summarize_tool_one 使用的原文链接，用于取回 title/tags/content")
    token: str = Field(description="登录凭证，来自 send_tool_two 的返回值")
    topic: str = Field(description= '搜索主题，只能是 "AI" 或 "Agent"')


class send_tool_two_params(BaseModel):
    """登录并获取 token，供 send_tool_one 使用。无参数。"""
    pass


class search_tool_one_params(BaseModel):
    """按主题搜索 GitHub 仓库，去重后写入本地 detail.jsonl 与 SearchFor{topic}NotUsed.jsonl。
    本地已有该主题数据时优先复用，不要重复搜索。topic 只能是 "AI" 或 "Agent"。"""
    topic: str = Field(description='搜索主题，只能是 "AI" 或 "Agent"')
    number1: int = Field(description="每次拉取的仓库数（per_page），用户有明确要求时按要求，最大为100")
    pagenumber: int = Field(description="拉取的页数，页数不同，拉取的内容也不同，取值为1到10")


class search_tool_two_params(BaseModel):
    """按 url 获取指定 GitHub 仓库的 README 正文并存入本地仓库。
    只返回截断预览，全文由程序侧保存，供 de_markdown_tool_one 使用。
    url 通常来自 select_tool_one 的返回列表。"""
    url: str = Field(description="GitHub 仓库链接，如 https://github.com/owner/repo")


class select_tool_one_params(BaseModel):
    """按 depth/breadth/popularity/recency 四个指标，从 SearchFor{topic}NotUsed.jsonl 中
    筛选 see=False 的 GitHub 项目，返回 url 列表。n 为本次读取并筛选的条数。
    指标取值只能是 very low / low / medium / high / very high。
    无法判断指标时使用默认示例值；结果不理想时可调整指标重试。"""
    topic: str = Field(description="主题，用于定位 SearchFor{topic}NotUsed.jsonl，只能是 AI 或 Agent")
    target_idea: str = Field(
        default='{"depth":"high","breadth":"medium","popularity":"high","recency":"high"}',
        description='四指标 JSON，取值 very low/low/medium/high/very high；无法判断时用默认值',
    )
    n: int = Field(ge=1, description="本次读取并筛选的条数，从 see=False 的记录中取前 n 条")


TOOLS = [
    pydantic_function_tool(ask_tool_one_params,         name="ask_tool_one"),
    pydantic_function_tool(reset_tool_one_params,       name="reset_tool_one"),
    pydantic_function_tool(get_message_tool_one_params, name="get_message_tool_one"),
    pydantic_function_tool(summarize_tool_one_params,   name="summarize_tool_one"),
    pydantic_function_tool(de_markdown_tool_one_params, name="de_markdown_tool_one"),
    pydantic_function_tool(send_tool_one_params,        name="send_tool_one"),
    pydantic_function_tool(send_tool_two_params,        name="send_tool_two"),
    pydantic_function_tool(search_tool_one_params,      name="search_tool_one"),
    pydantic_function_tool(search_tool_two_params,      name="search_tool_two"),
    pydantic_function_tool(select_tool_one_params,      name="select_tool_one"),
]

TOOL_REGISTRY = {
    "ask_tool_one":         AskToolOne.ask_tool_one,
    "reset_tool_one":       ResetToolOne.reset_tool_one,
    "get_message_tool_one": GetMessageToolOne.get_message_tool_one,
    "summarize_tool_one":   SummarizeToolOne.summarize_tool_one,
    "de_markdown_tool_one": DeMarkdownToolOne.de_markdown_tool_one,
    "send_tool_one":        SendToolOne.send_tool_one,
    "send_tool_two":        SendToolTwo.send_tool_two,
    "search_tool_one":      SearchToolOne.search_tool_one,
    "search_tool_two":      SearchToolTwo.search_tool_two,
    "select_tool_one":      SelectToolOne.select_tool_one,
}
