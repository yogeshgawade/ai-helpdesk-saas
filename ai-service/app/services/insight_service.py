import json

from app.llm.provider import LLMProvider
from app.models.insight import InsightRequest, InsightResponse


class InsightService:

    def __init__(self, llm_provider: LLMProvider):
        self.llm_provider = llm_provider

    def generate(
        self,
        request: InsightRequest,
    ) -> InsightResponse:

        system_prompt = """
You are an analytics assistant for a customer support helpdesk.

Analyze the supplied support analytics and produce a concise, useful
management insight.

Rules:
- Use ONLY the supplied data.
- Do not invent facts, causes, trends, or numbers.
- The supplied SQL-derived metrics are authoritative.
- Identify meaningful patterns in ticket volume, workload, SLA performance,
  response time, resolution time, priority, status, and category.
- If the data is insufficient to support a conclusion, say so.
- Do not mention that you are an AI.
- Do not use markdown headings.
- Keep the response to 2-4 concise sentences.
- Focus on actionable observations rather than generic advice.
""".strip()

        user_prompt = (
            "Here is the analytics snapshot:\n\n"
            + json.dumps(
                request.model_dump(),
                indent=2,
                default=str,
            )
        )

        insight, model = self.llm_provider.generate(
            system_prompt=system_prompt,
            user_prompt=user_prompt,
        )

        return InsightResponse(
            insight=insight.strip(),
            model=model,
        )
