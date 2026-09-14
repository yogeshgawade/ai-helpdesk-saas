from pathlib import Path

from docx import Document
from pypdf import PdfReader


SUPPORTED_EXTENSIONS = {".pdf", ".docx", ".txt", ".md", ".markdown"}


class UnsupportedDocumentError(ValueError):
    pass


def extract_text(file_path: str) -> str:
    """
    Extract plain text from a supported document.

    Supported formats:
    - PDF
    - DOCX
    - TXT
    - Markdown
    """
    path = Path(file_path)
    extension = path.suffix.lower()

    if extension not in SUPPORTED_EXTENSIONS:
        raise UnsupportedDocumentError(
            f"Unsupported document type: {extension or 'unknown'}"
        )

    if extension == ".pdf":
        return _extract_pdf(path)

    if extension == ".docx":
        return _extract_docx(path)

    return _extract_plain_text(path)


def _extract_pdf(path: Path) -> str:
    reader = PdfReader(path)

    pages = []

    for page in reader.pages:
        text = page.extract_text() or ""
        pages.append(text)

    return _normalize_text("\n".join(pages))


def _extract_docx(path: Path) -> str:
    document = Document(path)

    paragraphs = [
        paragraph.text
        for paragraph in document.paragraphs
        if paragraph.text.strip()
    ]

    return _normalize_text("\n".join(paragraphs))


def _extract_plain_text(path: Path) -> str:
    return _normalize_text(path.read_text(encoding="utf-8"))


def _normalize_text(text: str) -> str:
    lines = [line.strip() for line in text.splitlines()]

    non_empty_lines = [
        line
        for line in lines
        if line
    ]

    return "\n".join(non_empty_lines).strip()