import re

from md2text import md2text

from ..RestoreTool import RestoreToolOne

# 按 url 缓存提取出的句子列表。
finaldata = {}


def extract_sentences(text: str, *, min_sentence_len: int = 100) -> list[str]:
    """从 Markdown 文本中提取完整句子，返回句子列表。"""
    # 常见文件后缀，切句时保护后缀中的点不被误判为句号。
    file_extensions = [
        "tar.gz", "tar.bz2", "tar.xz",
        "jsx", "tsx", "mjs", "cjs", "json", "jsonl", "yaml", "yml", "toml", "ini", "cfg", "conf",
        "html", "htm", "css", "scss", "sass", "less",
        "md", "mdx", "rst", "txt", "log", "csv", "tsv",
        "py", "pyi", "ipynb", "rb", "php", "java", "kt", "kts", "scala", "go", "rs", "swift",
        "c", "h", "cc", "cpp", "cxx", "hpp", "hh", "cs", "fs", "fsx", "m", "mm",
        "sh", "bash", "zsh", "fish", "ps1", "bat", "cmd",
        "sql", "db", "sqlite", "parquet",
        "png", "jpg", "jpeg", "gif", "svg", "webp", "ico", "bmp", "tiff",
        "mp3", "wav", "flac", "ogg", "mp4", "mov", "avi", "mkv", "webm",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "zip", "rar", "7z", "gz", "bz2", "xz", "tar",
        "exe", "dll", "so", "dylib", "bin", "iso", "dmg", "apk", "ipa",
        "lock", "env", "gitignore", "dockerignore", "editorconfig",
    ]
    file_extensions = sorted(set(file_extensions), key=len, reverse=True)
    ext_pattern = re.compile(
        r"\.(" + "|".join(re.escape(e) for e in file_extensions) + r")(?![\w])",
        re.IGNORECASE,
    )
    ext_placeholder = "\u0000EXT\u0000"

    def protect_extensions(s: str) -> str:
        """把 .ext 替换为占位符，避免后缀中的点被当作句末。"""
        return ext_pattern.sub(lambda m: ext_placeholder + m.group(1), s)

    def restore_extensions(s: str) -> str:
        """把占位符还原为 .。"""
        return s.replace(ext_placeholder, ".")

    def strip_markdown(s: str) -> str:
        """逐类去除 Markdown 标记。"""
        s = re.sub(r"```.*?```", "", s, flags=re.S)
        s = re.sub(r"`[^`]*`", "", s)
        s = re.sub(r"!\[[^\]]*\]\([^)]*\)", "", s)
        s = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", s)
        s = re.sub(r"<[^>]+>", "", s)
        s = re.sub(r"^\s{0,3}#{1,6}\s*", "", s, flags=re.M)
        s = re.sub(r"^\s{0,3}>\s?", "", s, flags=re.M)
        s = re.sub(r"^\s{0,3}[-*+]\s+", "", s, flags=re.M)
        s = re.sub(r"^\s{0,3}\d+\.\s+", "", s, flags=re.M)
        s = re.sub(r"(\*\*|__|\*|_|~~)", "", s)
        s = re.sub(r"^\s*[-*_]{3,}\s*$", "", s, flags=re.M)
        return s

    def is_noise(line: str) -> bool:
        """判断是否为噪声行（空行、表格、分隔线、无文字的行）。"""
        s = line.strip()
        if not s:
            return True
        if s.startswith("|"):
            return True
        if re.fullmatch(r"[-=*_|:\s]+", s):
            return True
        if not re.search(r"[A-Za-z\u4e00-\u9fff]", s):
            return True
        return False

    cleaned = strip_markdown(text)
    sentences: list[str] = []
    buffer: list[str] = []

    def flush(buf: list[str]) -> None:
        """把缓冲区中的段落合并、切句、过滤后加入 sentences。"""
        if not buf:
            return
        para = " ".join(buf)
        para = protect_extensions(para)
        parts = re.split(r"(?<=[.!?。！？])", para)
        for p in parts:
            p = restore_extensions(p).strip()
            if not p:
                continue
            if not re.search(r"[.!?。！？]$", p):
                continue
            if len(re.sub(r"\s+", "", p)) < min_sentence_len:
                continue
            sentences.append(p)

    for line in cleaned.splitlines():
        if is_noise(line):
            flush(buffer)
            buffer = []
            continue
        buffer.append(line.strip())
    flush(buffer)

    return sentences


def tool1(text1):
    """把 Markdown 转成纯文本。"""
    text = md2text.to_text(text1)
    print("markdown文本已经解析成普通文本")
    return text


def de_markdown_tool_one(url, text1=None):
    """清理 README 的 Markdown 并按句提取，句子列表按 url 存入共享仓库。"""
    print("开始调用de_markdown_tool_one")

    if text1 is None:
        text1 = RestoreToolOne.get_search_tool_two_data(url)
        if not text1:
            print(f"de_markdown_tool_one调用失败，没有 url={url} 的 README，请先调用 search2")
            return {
                "status": "error",
                "stdout": None,
                "stderr": f"没有 url={url} 的 README，请先调用 search2",
            }

    # 反复降低最小句长阈值，直到提取到的句子总长占原文比例超过 10%。
    minbit = 100
    text2 = tool1(text1)
    num1 = len(text2)
    if num1 == 0:
        print("de_markdown_tool_one调用失败，帖子markdown不符合要求")
        return {
            "status": "error",
            "stdout": None,
            "stderr": "帖子markdown不符合要求",
        }

    result1 = extract_sentences(text2, min_sentence_len=minbit)
    while True:
        num2 = 0
        for i, s in enumerate(result1, 1):
            print(f"{i}\t{s}")
            num2 += len(s)
        print(f"\n共提取 {len(result1)} 句完整句子（最短 {minbit} 字符）")

        if num2 / num1 > 0.1:
            finaldata[url] = result1
            RestoreToolOne.add_data("de_markdown_tool_one", finaldata)
            print("de_markdown_tool_one结束调用，成功")
            return {
                "status": "success",
                "stdout": {
                    "length": num2,
                    "content": "".join(result1)[:200],
                },
                "stderr": None,
            }

        minbit -= 5
        if minbit <= 0:
            break
        result1 = extract_sentences(text2, min_sentence_len=minbit)

    print("de_markdown_tool_one调用失败，帖子markdown不符合要求")
    return {
        "status": "error",
        "stdout": None,
        "stderr": "帖子markdown不符合要求",
    }
