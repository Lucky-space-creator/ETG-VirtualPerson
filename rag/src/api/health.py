from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from datetime import datetime

router = APIRouter()

class HealthResponse(BaseModel):
    status: str
    service: str
    version: str
    timestamp: str

@router.get("/health", response_model=HealthResponse)
async def health_check():
    """健康检查"""
    return HealthResponse(
        status="ok",
        service="rag-embedding-service",
        version="1.0.0",
        timestamp=datetime.now().isoformat(),
    )
