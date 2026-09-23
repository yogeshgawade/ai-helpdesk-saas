import os
import tempfile
import uuid

from fastapi import FastAPI, File, UploadFile, Request
from fastapi.responses import StreamingResponse

from app.models.document import (
	DocumentChunkResponse,
	DocumentProcessResponse,
)
from app.models.embedding import EmbeddingRequest, EmbeddingResponse
from app.models.classification import ClassificationRequest, ClassificationResponse
from app.models.summarization import SummarizationRequest, SummarizationResponse
from app.models.insight import InsightRequest, InsightResponse
from app.services.chunker import chunk_text
from app.services.document_extractor import extract_text
from app.services.embedding_provider import LocalEmbeddingProvider


app = FastAPI(title="Helpdesk AI Service")


@app.middleware("http")
async def request_id_middleware(request: Request, call_next):
        request_id = request.headers.get("X-Request-Id")

        if request_id is None or not request_id.strip():
                request_id = str(uuid.uuid4())

        request.state.request_id = request_id

        response = await call_next(request)
        response.headers["X-Request-Id"] = request_id

        return response


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


from app.llm.factory import create_llm_provider
from app.models.rag import RagRequest, RagResponse
from app.models.response_assistant import ResponseAssistantRequest
from app.services.rag_service import RagService
from app.services.classification_service import ClassificationService
from app.services.summarization_service import SummarizationService
from app.services.response_assistant_service import ResponseAssistantService
from app.services.insight_service import InsightService


llm_provider = create_llm_provider()
rag_service = RagService(llm_provider)
classification_service = ClassificationService(llm_provider)
summarization_service = SummarizationService(llm_provider)
response_assistant_service = ResponseAssistantService(llm_provider)
insight_service = InsightService(llm_provider)


@app.post("/rag/query", response_model=RagResponse)
def generate_rag_response(request: RagRequest):
    return rag_service.generate(request)


@app.post("/response-assistant/suggest")
def suggest_response(request: ResponseAssistantRequest):
    return response_assistant_service.generate(request)


@app.post("/response-assistant/stream")
def stream_response(request: ResponseAssistantRequest):
    return StreamingResponse(
        response_assistant_service.generate_stream(request),
        media_type="application/x-ndjson",
    )


@app.post(
        "/classify",
        response_model=ClassificationResponse,
)
def classify_ticket(request: ClassificationRequest):
    return classification_service.classify(request)


@app.post(
        "/summarize",
        response_model=SummarizationResponse,
)
def summarize_ticket(request: SummarizationRequest):
    return summarization_service.summarize(request)


@app.post(
        "/insights",
        response_model=InsightResponse,
)
def generate_insight(request: InsightRequest):
    return insight_service.generate(request)
