import os
from collections.abc import Iterator

from google import genai
from google.genai import errors

from app.llm.provider import LLMProvider


class GeminiProvider(LLMProvider):

    def __init__(self):
        api_key = os.getenv("GEMINI_API_KEY")

        if not api_key:
            raise RuntimeError(
                "GEMINI_API_KEY environment variable is not configured"
            )

        self.primary_model = os.getenv(
            "LLM_MODEL",
            "gemini-3.6-flash",
        )

        self.fallback_model = os.getenv(
            "LLM_FALLBACK_MODEL",
            "gemini-3.5-flash-lite",
        )

        self.client = genai.Client(api_key=api_key)

    def generate(
        self,
        system_prompt: str,
        user_prompt: str,
    ) -> tuple[str, str]:

        try:
            return self._generate_with_model(
                self.primary_model,
                system_prompt,
                user_prompt,
            )

        except errors.ServerError as exception:
            if exception.code != 503:
                raise

            print(
                f"Primary Gemini model '{self.primary_model}' "
                f"returned 503. Falling back to "
                f"'{self.fallback_model}'."
            )

            return self._generate_with_model(
                self.fallback_model,
                system_prompt,
                user_prompt,
            )

    def generate_stream(
        self,
        system_prompt: str,
        user_prompt: str,
    ) -> tuple[str, Iterator[str]]:

        try:
            stream = self._generate_stream_with_model(
                self.primary_model,
                system_prompt,
                user_prompt,
            )

            return self.primary_model, stream

        except errors.ServerError as exception:
            if exception.code != 503:
                raise

            print(
                f"Primary Gemini model '{self.primary_model}' "
                f"returned 503. Falling back to "
                f"'{self.fallback_model}'."
            )

            stream = self._generate_stream_with_model(
                self.fallback_model,
                system_prompt,
                user_prompt,
            )

            return self.fallback_model, stream

    def _generate_with_model(
        self,
        model: str,
        system_prompt: str,
        user_prompt: str,
    ) -> tuple[str, str]:

        response = self.client.models.generate_content(
            model=model,
            contents=user_prompt,
            config={
                "system_instruction": system_prompt,
            },
        )

        if not response.text:
            raise RuntimeError(
                f"LLM model '{model}' returned an empty response"
            )

        return response.text, model

    def _generate_stream_with_model(
        self,
        model: str,
        system_prompt: str,
        user_prompt: str,
    ) -> Iterator[str]:

        response_stream = self.client.models.generate_content_stream(
            model=model,
            contents=user_prompt,
            config={
                "system_instruction": system_prompt,
            },
        )

        for response in response_stream:
            if response.text:
                yield response.text
