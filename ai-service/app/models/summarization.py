from pydantic import BaseModel


class SummarizationMessage(BaseModel):
    role: str
    content: str


class SummarizationRequest(BaseModel):
    subject: str
    messages: list[SummarizationMessage]


class SummarizationResponse(BaseModel):
    summary: str
