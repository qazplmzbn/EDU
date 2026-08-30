from typing import Dict, List, Optional
from pydantic import BaseModel, Field

class DimktInteraction(BaseModel):
    interactionSeq: int
    knowledgeWeights: Dict[str, float]
    scoreNormalized: float = Field(ge=0, le=1)
    questionDifficulty: float = Field(ge=0, le=1)
    questionPurpose: str
    cognitiveLevel: Optional[str] = None

class DimktInferRequest(BaseModel):
    userId: int
    courseId: int
    modelVersion: str
    knowledgeIndexVersion: str
    previousStateRef: Optional[str] = None
    interactions: List[DimktInteraction]

class DimktInferenceResponse(BaseModel):
    masteryHead: Dict[str, float]
    confidence: Dict[str, float]
    stateRef: str
    processedThroughSeq: int
    modelVersion: str
    knowledgeIndexVersion: str

class DimktRecalibrateRequest(DimktInferRequest):
    previousStateRef: Optional[str] = None
