from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_create_embedding():
    response = client.post(
        "/embeddings",
        json={"text": "Refunds are available within 30 days."},
    )

    assert response.status_code == 200

    data = response.json()

    assert data["dimension"] == 384
    assert len(data["embedding"]) == 384
    assert all(isinstance(value, float) for value in data["embedding"])
