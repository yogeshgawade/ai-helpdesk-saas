-- ============================================================
-- AI Helpdesk SaaS - Initial Database Schema
-- Flyway migration V1
-- ============================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;


-- ============================================================
-- ENUM TYPES
-- ============================================================

CREATE TYPE organization_plan AS ENUM (
    'FREE',
    'PRO',
    'ENTERPRISE'
);

CREATE TYPE membership_role AS ENUM (
    'OWNER',
    'ADMIN',
    'AGENT',
    'CUSTOMER'
);

CREATE TYPE ticket_status AS ENUM (
    'OPEN',
    'IN_PROGRESS',
    'PENDING',
    'RESOLVED',
    'CLOSED'
);

CREATE TYPE ticket_priority AS ENUM (
    'LOW',
    'MEDIUM',
    'HIGH',
    'URGENT'
);

CREATE TYPE kb_document_status AS ENUM (
    'PROCESSING',
    'READY',
    'FAILED'
);

CREATE TYPE ai_generation_kind AS ENUM (
    'RESPONSE_SUGGESTION',
    'SUMMARY',
    'INSIGHT'
);


-- ============================================================
-- ORGANIZATIONS
-- ============================================================

CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    plan organization_plan NOT NULL DEFAULT 'FREE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- USERS
-- ============================================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_users_email_lower
    ON users (LOWER(email));


-- ============================================================
-- MEMBERSHIPS
-- ============================================================

CREATE TABLE memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    role membership_role NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_membership_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_membership_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_membership_user_organization
        UNIQUE (user_id, organization_id)
);

CREATE INDEX idx_memberships_organization_role
    ON memberships (organization_id, role);


-- ============================================================
-- SLA POLICIES
-- ============================================================

CREATE TABLE sla_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    first_response_minutes INTEGER NOT NULL,
    resolution_minutes INTEGER NOT NULL,
    priority ticket_priority NOT NULL,

    CONSTRAINT fk_sla_policy_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_sla_first_response_positive
        CHECK (first_response_minutes > 0),

    CONSTRAINT chk_sla_resolution_positive
        CHECK (resolution_minutes > 0)
);

CREATE INDEX idx_sla_policies_organization
    ON sla_policies (organization_id);


-- ============================================================
-- TICKETS
-- ============================================================

CREATE TABLE tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    assigned_agent_id UUID,
    subject VARCHAR(500) NOT NULL,
    status ticket_status NOT NULL DEFAULT 'OPEN',
    priority ticket_priority NOT NULL DEFAULT 'MEDIUM',
    category VARCHAR(100),
    sla_policy_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,

    CONSTRAINT fk_ticket_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ticket_customer
        FOREIGN KEY (customer_id)
        REFERENCES users(id),

    CONSTRAINT fk_ticket_assigned_agent
        FOREIGN KEY (assigned_agent_id)
        REFERENCES users(id),

    CONSTRAINT fk_ticket_sla_policy
        FOREIGN KEY (sla_policy_id)
        REFERENCES sla_policies(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_tickets_organization_status_priority
    ON tickets (
        organization_id,
        status,
        priority,
        created_at DESC,
        id DESC
    );

CREATE INDEX idx_tickets_organization_assigned_agent
    ON tickets (
        organization_id,
        assigned_agent_id
    );

CREATE INDEX idx_tickets_organization_created
    ON tickets (
        organization_id,
        created_at DESC,
        id DESC
    );


-- ============================================================
-- TICKET MESSAGES
-- ============================================================

CREATE TABLE ticket_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL,
    author_id UUID NOT NULL,
    body TEXT NOT NULL,
    is_internal_note BOOLEAN NOT NULL DEFAULT FALSE,
    is_ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ticket_message_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES tickets(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ticket_message_author
        FOREIGN KEY (author_id)
        REFERENCES users(id)
);

CREATE INDEX idx_ticket_messages_ticket_created
    ON ticket_messages (
        ticket_id,
        created_at,
        id
    );


-- ============================================================
-- TICKET ATTACHMENTS
-- ============================================================

CREATE TABLE ticket_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_message_id UUID NOT NULL,
    s3_key VARCHAR(1024) NOT NULL,
    filename VARCHAR(500) NOT NULL,
    mime_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    scanned_at TIMESTAMPTZ,
    scan_result VARCHAR(50),

    CONSTRAINT fk_attachment_message
        FOREIGN KEY (ticket_message_id)
        REFERENCES ticket_messages(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_attachment_size
        CHECK (size_bytes >= 0)
);

CREATE INDEX idx_ticket_attachments_message
    ON ticket_attachments (ticket_message_id);


-- ============================================================
-- TICKET EVENTS / AUDIT TRAIL
-- ============================================================

CREATE TABLE ticket_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    actor_id UUID,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ticket_event_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES tickets(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ticket_event_actor
        FOREIGN KEY (actor_id)
        REFERENCES users(id)
);

CREATE INDEX idx_ticket_events_ticket_created
    ON ticket_events (
        ticket_id,
        created_at,
        id
    );


-- ============================================================
-- TAGS
-- ============================================================

CREATE TABLE tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,

    CONSTRAINT fk_tag_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_tag_organization_name
        UNIQUE (organization_id, name)
);

CREATE INDEX idx_tags_organization
    ON tags (organization_id);


-- ============================================================
-- TICKET TAGS
-- ============================================================

CREATE TABLE ticket_tags (
    ticket_id UUID NOT NULL,
    tag_id UUID NOT NULL,

    PRIMARY KEY (ticket_id, tag_id),

    CONSTRAINT fk_ticket_tag_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES tickets(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ticket_tag_tag
        FOREIGN KEY (tag_id)
        REFERENCES tags(id)
        ON DELETE CASCADE
);


-- ============================================================
-- KNOWLEDGE BASE DOCUMENTS
-- ============================================================

CREATE TABLE knowledge_base_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    s3_key VARCHAR(1024),
    content_hash VARCHAR(128),
    status kb_document_status NOT NULL DEFAULT 'PROCESSING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_kb_document_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_kb_documents_organization_status
    ON knowledge_base_documents (
        organization_id,
        status
    );


-- ============================================================
-- KNOWLEDGE BASE CHUNKS
-- ============================================================

CREATE TABLE kb_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding VECTOR(1536) NOT NULL,
    chunk_index INTEGER NOT NULL,
    token_count INTEGER,

    CONSTRAINT fk_kb_chunk_document
        FOREIGN KEY (document_id)
        REFERENCES knowledge_base_documents(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_kb_chunk_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_kb_chunk_index
        CHECK (chunk_index >= 0),

    CONSTRAINT chk_kb_chunk_token_count
        CHECK (token_count IS NULL OR token_count > 0),

    CONSTRAINT uq_kb_chunk_document_index
        UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_kb_chunks_organization
    ON kb_chunks (organization_id);

CREATE INDEX idx_kb_chunks_document
    ON kb_chunks (document_id);

-- Vector similarity search index
CREATE INDEX idx_kb_chunks_embedding_hnsw
    ON kb_chunks
    USING hnsw (embedding vector_cosine_ops);


-- ============================================================
-- AI CLASSIFICATIONS
-- ============================================================

CREATE TABLE ai_classifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL,
    category VARCHAR(100),
    priority_suggestion ticket_priority,
    sentiment VARCHAR(50),
    confidence NUMERIC(5, 4),
    model_version VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_classification_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES tickets(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_ai_classification_confidence
        CHECK (
            confidence IS NULL
            OR (confidence >= 0 AND confidence <= 1)
        )
);

CREATE INDEX idx_ai_classifications_ticket_created
    ON ai_classifications (
        ticket_id,
        created_at DESC
    );


-- ============================================================
-- AI GENERATIONS
-- ============================================================

CREATE TABLE ai_generations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID,
    kind ai_generation_kind NOT NULL,
    prompt_hash VARCHAR(128),
    output TEXT NOT NULL,
    model VARCHAR(255) NOT NULL,
    token_count INTEGER,
    latency_ms BIGINT,
    approved_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_generation_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES tickets(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ai_generation_approved_by
        FOREIGN KEY (approved_by)
        REFERENCES users(id),

    CONSTRAINT chk_ai_generation_token_count
        CHECK (token_count IS NULL OR token_count >= 0),

    CONSTRAINT chk_ai_generation_latency
        CHECK (latency_ms IS NULL OR latency_ms >= 0)
);

CREATE INDEX idx_ai_generations_ticket_created
    ON ai_generations (
        ticket_id,
        created_at DESC
    );

CREATE INDEX idx_ai_generations_kind_created
    ON ai_generations (
        kind,
        created_at DESC
    );


-- ============================================================
-- NOTIFICATIONS
-- ============================================================

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user_read_created
    ON notifications (
        user_id,
        read_at,
        created_at DESC
    );


-- ============================================================
-- APPLICATION AUDIT LOG
-- ============================================================

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    actor_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_log_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_audit_log_actor
        FOREIGN KEY (actor_id)
        REFERENCES users(id)
);

CREATE INDEX idx_audit_logs_organization_created
    ON audit_logs (
        organization_id,
        created_at DESC
    );

CREATE INDEX idx_audit_logs_entity
    ON audit_logs (
        entity_type,
        entity_id,
        created_at DESC
    );
