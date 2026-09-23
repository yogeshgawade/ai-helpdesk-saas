import os

from app.llm.provider import LLMProvider


def create_llm_provider() -> LLMProvider:
    provider_name = os.getenv("LLM_PROVIDER", "").strip().lower()

    if not provider_name:
        raise RuntimeError(
            "LLM_PROVIDER environment variable is not configured"
        )

    if provider_name == "gemini":
        from app.llm.providers.gemini import GeminiProvider

        return GeminiProvider()

    raise RuntimeError(
        f"Unsupported LLM provider: {provider_name}"
    )
