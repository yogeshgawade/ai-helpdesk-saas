CREATE TABLE analytics_ai_insights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    insight TEXT NOT NULL,
    model VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_analytics_ai_insight_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_analytics_ai_insight_date_range
        CHECK (from_date <= to_date)
);

CREATE INDEX idx_analytics_ai_insights_organization_created
    ON analytics_ai_insights (
        organization_id,
        created_at DESC
    );
