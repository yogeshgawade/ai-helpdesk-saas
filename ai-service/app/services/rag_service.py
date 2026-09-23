from app.llm.provider import LLMProvider
from app.models.rag import RagCitation, RagRequest, RagResponse


class RagService:

    def __init__(self, llm_provider: LLMProvider):
        self.llm_provider = llm_provider

    def generate(self, request: RagRequest) -> RagResponse:
        context = self._build_context(request)

        system_prompt = """
You are an AI customer support assistant.

Answer the user's question using only the provided knowledge-base context.

Rules:
- Do not invent information.
- If the context does not contain enough information to answer,
  say that the knowledge base does not provide enough information.
- Keep the answer concise and helpful.
- Use the provided context as the source of truth.
""".strip()

        user_prompt = f"""
Knowledge-base context:

{context}

User question:

{request.query}
""".strip()

        answer, _ = self.llm_provider.generate(
            system_prompt=system_prompt,
            user_prompt=user_prompt,
        )

        citations = [
            RagCitation(
                document_id=chunk.document_id,
                document_title=chunk.document_title,
                chunk_id=chunk.id,
                chunk_index=chunk.chunk_index,
            )
            for chunk in request.chunks
        ]

        return RagResponse(
            answer=answer,
            citations=citations,
        )

    def _build_context(self, request: RagRequest) -> str:
        sections = []

        for chunk in request.chunks:
            sections.append(
                f"[Source: {chunk.document_title}, "
                f"chunk {chunk.chunk_index}]\n"
                f"{chunk.chunk_text}"
            )

        return "\n\n".join(sections)
