from app.services.embedding_provider import LocalEmbeddingProvider


def test_local_embedding_dimension():
    provider = LocalEmbeddingProvider()

    vector = provider.embed("Refunds are available within 30 days.")

    assert len(vector) == 384
    assert provider.dimension() == 384