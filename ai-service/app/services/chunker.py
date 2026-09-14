from dataclasses import dataclass


@dataclass(frozen=True)
class TextChunk:
    text: str
    chunk_index: int
    token_count: int


def estimate_tokens(text: str) -> int:
    """
    Rough token estimate.

    This is intentionally approximate.
    The exact tokenizer can be introduced when we
    choose the embedding model.
    """
    if not text.strip():
        return 0

    return max(1, len(text.split()) * 4 // 3)


def chunk_text(
    text: str,
    max_tokens: int = 500,
    overlap_tokens: int = 75,
) -> list[TextChunk]:
    """
    Split text into paragraph-aware chunks.

    Paragraph boundaries are preserved whenever possible.
    Chunks may overlap by approximately overlap_tokens.
    """
    if max_tokens <= 0:
        raise ValueError("max_tokens must be greater than 0")

    if overlap_tokens < 0:
        raise ValueError("overlap_tokens cannot be negative")

    if overlap_tokens >= max_tokens:
        raise ValueError("overlap_tokens must be smaller than max_tokens")

    paragraphs = [
        paragraph.strip()
        for paragraph in text.split("\n")
        if paragraph.strip()
    ]

    if not paragraphs:
        return []

    chunks: list[TextChunk] = []
    current_paragraphs: list[str] = []
    current_tokens = 0

    for paragraph in paragraphs:
        paragraph_tokens = estimate_tokens(paragraph)

        if paragraph_tokens > max_tokens:
            if current_paragraphs:
                chunks.append(
                    _create_chunk(
                        current_paragraphs,
                        len(chunks),
                    )
                )
                current_paragraphs = []
                current_tokens = 0

            large_chunks = _split_large_paragraph(
                paragraph,
                max_tokens,
                overlap_tokens,
                len(chunks),
            )

            chunks.extend(large_chunks)
            continue

        if (
            current_paragraphs
            and current_tokens + paragraph_tokens > max_tokens
        ):
            chunks.append(
                _create_chunk(
                    current_paragraphs,
                    len(chunks),
                )
            )

            overlap_paragraphs: list[str] = []
            overlap_count = 0

            for previous in reversed(current_paragraphs):
                previous_tokens = estimate_tokens(previous)

                if overlap_count + previous_tokens > overlap_tokens:
                    break

                overlap_paragraphs.insert(0, previous)
                overlap_count += previous_tokens

            current_paragraphs = overlap_paragraphs
            current_tokens = overlap_count

        current_paragraphs.append(paragraph)
        current_tokens += paragraph_tokens

    if current_paragraphs:
        chunks.append(
            _create_chunk(
                current_paragraphs,
                len(chunks),
            )
        )

    return chunks


def _create_chunk(
    paragraphs: list[str],
    index: int,
) -> TextChunk:
    text = "\n\n".join(paragraphs)

    return TextChunk(
        text=text,
        chunk_index=index,
        token_count=estimate_tokens(text),
    )


def _split_large_paragraph(
    paragraph: str,
    max_tokens: int,
    overlap_tokens: int,
    starting_index: int,
) -> list[TextChunk]:
    words = paragraph.split()

    chunks: list[TextChunk] = []

    max_words = max(1, max_tokens * 3 // 4)
    overlap_words = min(
        max_words - 1,
        overlap_tokens * 3 // 4,
    )

    start = 0

    while start < len(words):
        end = min(start + max_words, len(words))

        chunk_text_value = " ".join(words[start:end])

        chunks.append(
            TextChunk(
                text=chunk_text_value,
                chunk_index=starting_index + len(chunks),
                token_count=estimate_tokens(chunk_text_value),
            )
        )

        if end == len(words):
            break

        start = end - overlap_words

    return chunks