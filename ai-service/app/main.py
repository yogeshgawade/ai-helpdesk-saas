import os
import tempfile

from fastapi import FastAPI, File, UploadFile, Request

from app.models.document import (
	DocumentChunkResponse,
	DocumentProcessResponse,
)
from app.models.embedding import EmbeddingRequest, EmbeddingResponse
from app.services.chunker import chunk_text
from app.services.document_extractor import extract_text
from app.services.embedding_provider import LocalEmbeddingProvider


app = FastAPI(title="Helpdesk AI Service")

embedding_provider = LocalEmbeddingProvider()


@app.get("/health")
def health():
	return {"status": "ok"}


@app.post("/embeddings", response_model=EmbeddingResponse)
async def create_embedding(request: Request):
	body = await request.body()

	print("RAW EMBEDDING BODY:", body)

	request_data = await request.json()
	embedding_request = EmbeddingRequest(**request_data)

	vector = embedding_provider.embed(embedding_request.text)

	return EmbeddingResponse(
		embedding=vector,
		dimension=len(vector),
	)


@app.post(
	"/documents/process",
	response_model=DocumentProcessResponse,
)
def process_document(
	file: UploadFile = File(...),
):
	suffix = os.path.splitext(file.filename or "")[1]

	with tempfile.NamedTemporaryFile(
		suffix=suffix,
		delete=False,
	) as temporary_file:
		temporary_file.write(file.file.read())
		temporary_path = temporary_file.name

	try:
		text = extract_text(temporary_path)
		chunks = chunk_text(text)

		return DocumentProcessResponse(
			chunks=[
				DocumentChunkResponse(
					text=chunk.text,
					chunk_index=chunk.chunk_index,
					token_count=chunk.token_count,
				)
				for chunk in chunks
			]
		)
	finally:
		os.unlink(temporary_path)
