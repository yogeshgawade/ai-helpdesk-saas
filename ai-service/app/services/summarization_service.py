from app.llm.provider import LLMProvider
from app.models.summarization import (
    SummarizationRequest,
    SummarizationResponse,
)


class SummarizationService:

    def __init__(self, llm_provider: LLMProvider):
        self.llm_provider = llm_provider

    def summarize(
        self,
        request: SummarizationRequest,
    ) -> SummarizationResponse:

        system_prompt = """
You are an AI customer support ticket summarization system.

Create a concise summary of the customer support conversation.

The summary should:
- Explain the customer's main issue or request.
- Include important relevant details.
- Mention important actions or responses already provided.
- Mention the current state or unresolved issue when clear.
- Be factual and avoid inventing information.
- Ignore irrelevant conversational details.
- Do not include internal notes because they are not provided to you.

Return ONLY the summary text.
Do not use markdown.
""".strip()

        conversation = "\n\n".join(
            f"{message.role.upper()}: {message.content}"
            for message in request.messages
        )

        user_prompt = f"""
Ticket subject:
{request.subject}

Conversation:
{conversation}
""".strip()

        summary, _ = self.llm_provider.generate(
            system_prompt=system_prompt,
            user_prompt=user_prompt,
        )

        return SummarizationResponse(
            summary=summary.strip(),
        )
