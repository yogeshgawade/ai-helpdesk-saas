ALTER TABLE tickets
    ADD COLUMN first_response_due_at TIMESTAMPTZ,
    ADD COLUMN resolution_due_at TIMESTAMPTZ,
    ADD COLUMN first_responded_at TIMESTAMPTZ,
    ADD COLUMN sla_first_response_breached BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN sla_resolution_breached BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN sla_first_response_breached_at TIMESTAMPTZ,
    ADD COLUMN sla_resolution_breached_at TIMESTAMPTZ;

CREATE INDEX idx_tickets_sla_first_response_due
    ON tickets (first_response_due_at)
    WHERE first_responded_at IS NULL
      AND sla_first_response_breached = FALSE;

CREATE INDEX idx_tickets_sla_resolution_due
    ON tickets (resolution_due_at)
    WHERE resolved_at IS NULL
      AND sla_resolution_breached = FALSE;
