from pydantic import BaseModel, Field


class RagContextChunk(BaseModel):
    id: str
    document_id: str
    document_title: str
    chunk_text: str
    chunk_index: int
    similarity: float


class RagRequest(BaseModel):
    query: str = Field(min_length=1)
    chunks: list[RagContextChunk] = Field(min_length=1)


class RagCitation(BaseModel):
    document_id: str
    document_title: str
    chunk_id: str
    chunk_index: int


class RagResponse(BaseModel):
    answer: str
    citations: list[RagCitation]
