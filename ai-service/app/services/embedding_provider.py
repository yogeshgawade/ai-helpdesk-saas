from abc import ABC, abstractmethod
import os

import numpy as np
from sentence_transformers import SentenceTransformer


class EmbeddingProvider(ABC):
    @abstractmethod
    def embed(self, text: str) -> list[float]:
        pass

    @abstractmethod
    def dimension(self) -> int:
        pass


class LocalEmbeddingProvider(EmbeddingProvider):
    MODEL_NAME = "BAAI/bge-small-en-v1.5"

    def __init__(self):
        device = os.getenv("EMBEDDING_DEVICE", "cpu")

        self.model = SentenceTransformer(
            self.MODEL_NAME,
            device=device,
        )

    def embed(self, text: str) -> list[float]:
        vector = self.model.encode(text)

        return np.asarray(vector, dtype=np.float32).tolist()

    def dimension(self) -> int:
        return 384