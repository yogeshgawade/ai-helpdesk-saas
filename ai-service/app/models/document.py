from pydantic import BaseModel


class DocumentChunkResponse(BaseModel):
    text: str
    chunk_index: int
    token_count: int


class DocumentProcessResponse(BaseModel):
    chunks: list[DocumentChunkResponse]