

## 启动方式

```powershell
cd model-center-service
python -m venv .venv
.\.venv\Scripts\pip install -r requirements.txt
.\.venv\Scripts\uvicorn src.app:app --host 0.0.0.0 --port 8000 --reload
```

## 数据库配置

将 `deploy/model_center_schema.sql` 执行到项目使用的同一个 MySQL 实例中。默认配置如下：

```text
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=200124
DB_NAME=question_bank
```

## DIMKT 推理服务

生产 DIMKT 主机额外安装 `requirements-dimkt.txt`。训练输入只能来自已校验的 `resource_interaction` 导出：

```powershell
pip install -r requirements-dimkt.txt
python scripts/train_dimkt.py --input interactions.jsonl --knowledge-index knowledge-index.json --output-dir models/dimkt --model-version dimkt_v1 --knowledge-index-version knowledge_index_v1
```

设置 `DIMKT_MODEL_DIR` 后可调用 `/internal/v1/dimkt/infer`、`/recalibrate` 和 `/models/{version}/health`。服务会校验 manifest、知识点索引版本和权重 SHA-256；校验失败时不会声明模型可用。

