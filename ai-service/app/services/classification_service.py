import json

from app.llm.provider import LLMProvider
from app.models.classification import (
    ClassificationRequest,
    ClassificationResponse,
)


class ClassificationService:

    def __init__(self, llm_provider: LLMProvider):
        self.llm_provider = llm_provider

    def classify(
        self,
        request: ClassificationRequest,
    ) -> ClassificationResponse:

        system_prompt = """
You are an AI ticket classification system for a customer support helpdesk.

Classify the ticket into:
- category: one concise category such as account, billing, technical, access,
  security, refund, shipping, or other.
- priority: one of LOW, MEDIUM, HIGH, URGENT.
- confidence: a number between 0 and 1.
- reason: a short explanation.

Return ONLY valid JSON with exactly these fields:
{
  "category": "...",
  "priority": "...",
  "confidence": 0.0,
  "reason": "..."
}

Do not include markdown or code fences.
""".strip()

        user_prompt = f"""
Ticket subject:
{request.subject}

Existing priority:
{request.priority or "not provided"}

Existing category:
{request.category or "not provided"}
""".strip()

        raw_response = self.llm_provider.generate(
            system_prompt=system_prompt,
            user_prompt=user_prompt,
        )

        try:
            data = json.loads(raw_response)
        except json.JSONDecodeError as exception:
            raise RuntimeError(
                f"LLM returned invalid classification JSON: {raw_response}"
            ) from exception

        return ClassificationResponse(**data)
