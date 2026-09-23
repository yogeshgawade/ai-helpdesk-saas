from abc import ABC, abstractmethod
from collections.abc import Iterator


class LLMProvider(ABC):

    @abstractmethod
    def generate(
        self,
        system_prompt: str,
        user_prompt: str,
    ) -> tuple[str, str]:
        pass

    @abstractmethod
    def generate_stream(
        self,
        system_prompt: str,
        user_prompt: str,
    ) -> tuple[str, Iterator[str]]:
        pass
