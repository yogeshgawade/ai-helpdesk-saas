from pydantic import BaseModel, Field


class EmbeddingRequest(BaseModel):
    text: str = Field(min_length=1)


class EmbeddingResponse(BaseModel):
    embedding: list[float]
    dimension: int
