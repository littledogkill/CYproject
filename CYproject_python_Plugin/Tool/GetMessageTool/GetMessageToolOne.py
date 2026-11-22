import json
from pathlib import Path

# 数据目录：项目根目录下的 data。
DATA_DIR = Path(__file__).resolve().parents[2] / "data"


def get_message_tool_one(topic):
    """查询某主题的搜索数据统计：总数、未筛选数、已筛选数。"""
    path1 = DATA_DIR / f"SearchFor{topic}NotUsed.jsonl"
    path2 = DATA_DIR / "detail.jsonl"
    try:
        number1 = 0
        number2 = 0
        number3 = 0

        with path1.open("r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                obj = json.loads(line)
                if obj.get("see") is True:
                    number2 += 1
                number1 += 1

        with path2.open("r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                obj = json.loads(line)
                if obj.get("search") == topic:
                    number3 = obj.get("number")
                    break

        return {
            "status": "success",
            "stdout": {
                "total": number3,
                "not_used": number1 - number2,
                "selected": number2,
            },
            "stderr": None,
        }
    except Exception as e:
        return {
            "status": "error",
            "stdout": None,
            "stderr": str(e),
        }
