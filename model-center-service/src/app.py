from contextlib import asynccontextmanager

import aiomysql
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from src.core.config import settings
from src.routes.models import router as models_router
from src.services.model_service import ModelService
from src.services.dimkt_service import DimktService
from src.routes.dimkt import router as dimkt_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    mysql_pool = await aiomysql.create_pool(
        host=settings.DB_HOST,
        port=settings.DB_PORT,
        user=settings.DB_USER,
        password=settings.DB_PASSWORD,
        db=settings.DB_NAME,
        minsize=settings.MYSQL_POOL_MIN,
        maxsize=settings.MYSQL_POOL_MAX,
        autocommit=True,
        init_command="SET NAMES utf8mb4",
        use_unicode=True,
        charset="utf8mb4",
    )
    app.state.mysql_db = mysql_pool
    app.state.model_service = ModelService(mysql_pool)
    app.state.dimkt_service = DimktService()
    try:
        yield
    finally:
        mysql_pool.close()
        await mysql_pool.wait_closed()


app = FastAPI(title="Jinjin Model Center Service", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(models_router)
app.include_router(dimkt_router)


@app.get("/health")
async def health():
    return {"success": True, "data": {"status": "ok"}}
