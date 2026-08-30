from fastapi import APIRouter, HTTPException, Request
from src.schemas.dimkt import DimktInferRequest, DimktRecalibrateRequest

router = APIRouter(prefix="/internal/v1/dimkt", tags=["dimkt"])

@router.post("/infer")
async def infer(payload: DimktInferRequest, request: Request):
    try:
        return {"success": True, "data": request.app.state.dimkt_service.infer(payload).model_dump()}
    except ValueError as exc:
        raise HTTPException(status_code=409, detail=str(exc))
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc))

@router.post("/recalibrate")
async def recalibrate(payload: DimktRecalibrateRequest, request: Request):
    try:
        return {"success": True, "data": request.app.state.dimkt_service.infer(payload, recalibrate=True).model_dump()}
    except ValueError as exc:
        raise HTTPException(status_code=409, detail=str(exc))
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc))

@router.get("/models/{version}/health")
async def health(version: str, request: Request):
    result = request.app.state.dimkt_service.health(version)
    return {"success": result["ready"], "data": result}
