ALTER TABLE tickets
    ADD COLUMN ai_summary TEXT,
    ADD COLUMN ai_summarized_at TIMESTAMP WITH TIME ZONE;
