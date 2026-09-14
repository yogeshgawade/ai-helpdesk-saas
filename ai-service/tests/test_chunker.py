import pytest

from app.services.chunker import chunk_text, estimate_tokens


def test_empty_text_returns_no_chunks():
    assert chunk_text("") == []


def test_short_text_returns_one_chunk():
    text = "Refunds are available within 30 days."

    chunks = chunk_text(text)

    assert len(chunks) == 1
    assert chunks[0].text == text
    assert chunks[0].chunk_index == 0


def test_paragraphs_are_grouped_into_chunks():
    text = "\n".join(
        [
            "Paragraph one.",
            "Paragraph two.",
            "Paragraph three.",
        ]
    )

    chunks = chunk_text(
        text,
        max_tokens=20,
        overlap_tokens=3,
    )

    assert len(chunks) >= 1
    assert chunks[0].chunk_index == 0

    for index, chunk in enumerate(chunks):
        assert chunk.chunk_index == index
        assert chunk.token_count == estimate_tokens(chunk.text)


def test_chunks_have_ordered_indexes():
    text = "\n".join(
        f"Paragraph {i} containing some useful information."
        for i in range(20)
    )

    chunks = chunk_text(
        text,
        max_tokens=20,
        overlap_tokens=3,
    )

    assert [chunk.chunk_index for chunk in chunks] == list(
        range(len(chunks))
    )


def test_large_paragraph_is_split():
    text = " ".join(
        f"word{i}"
        for i in range(1000)
    )

    chunks = chunk_text(
        text,
        max_tokens=100,
        overlap_tokens=10,
    )

    assert len(chunks) > 1

    for chunk in chunks:
        assert chunk.text.strip()
        assert chunk.token_count > 0


def test_invalid_configuration():
    with pytest.raises(ValueError):
        chunk_text("hello", max_tokens=0)

    with pytest.raises(ValueError):
        chunk_text("hello", max_tokens=100, overlap_tokens=100)

    with pytest.raises(ValueError):
        chunk_text("hello", max_tokens=100, overlap_tokens=-1)