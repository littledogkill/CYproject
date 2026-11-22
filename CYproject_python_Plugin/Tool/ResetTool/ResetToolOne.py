import json
from pathlib import Path

# 数据目录：项目根目录下的 data。
DATA_DIR = Path(__file__).resolve().parents[2] / "data"


def reset_tool_one(topic):
    """把某主题下所有记录的 see 置回 False，使其重新变为未筛选状态。"""
    try:
        path1 = DATA_DIR / f"SearchFor{topic}NotUsed.jsonl"
        data = []
        with path1.open("r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line:
                    data.append(json.loads(line))

        for d in data:
            d["see"] = False

        with path1.open("w", encoding="utf-8") as f:
            for d in data:
                f.write(json.dumps(d, ensure_ascii=False) + "\n")

        return {
            "status": "success",
            "stdout": "success",
            "stderr": None,
        }
    except Exception as e:
        return {
            "status": "error",
            "stdout": None,
            "stderr": str(e),
        }
