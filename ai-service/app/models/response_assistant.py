from pydantic import BaseModel, Field


class ResponseAssistantMessage(BaseModel):
    body: str = Field(min_length=1)
    internal_note: bool


class ResponseAssistantRequest(BaseModel):
    ticket_subject: str = Field(min_length=1)
    ticket_priority: str
    ticket_category: str | None = None
    messages: list[ResponseAssistantMessage] = Field(min_length=1)
    chunks: list[dict] = Field(min_length=1)


class ResponseAssistantResponse(BaseModel):
    answer: str
    model: str
    citations: list[dict]
