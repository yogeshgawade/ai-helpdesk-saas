from pydantic import BaseModel, Field


class ClassificationRequest(BaseModel):
    subject: str = Field(min_length=1)
    priority: str | None = None
    category: str | None = None


class ClassificationResponse(BaseModel):
    category: str
    priority: str
    confidence: float
    reason: str
