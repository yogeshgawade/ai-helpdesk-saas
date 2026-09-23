ALTER TABLE tickets
    ADD COLUMN ai_category VARCHAR(100),
    ADD COLUMN ai_priority VARCHAR(20),
    ADD COLUMN ai_confidence DOUBLE PRECISION,
    ADD COLUMN ai_reason TEXT,
    ADD COLUMN ai_classified_at TIMESTAMP WITH TIME ZONE;
