DROP INDEX IF EXISTS idx_kb_chunks_embedding_hnsw;

DELETE FROM kb_chunks;

ALTER TABLE kb_chunks
    ALTER COLUMN embedding TYPE VECTOR(384);

CREATE INDEX idx_kb_chunks_embedding_hnsw
    ON kb_chunks
    USING hnsw (embedding vector_cosine_ops);