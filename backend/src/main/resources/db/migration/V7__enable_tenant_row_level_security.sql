-- ============================================================
-- Tenant Row-Level Security
-- Flyway migration V7
--
-- The application uses a shared PostgreSQL role that owns these
-- tables, so FORCE ROW LEVEL SECURITY is required.
--
-- Tenant context is supplied by:
--   app.current_org_id
--
-- The application sets this value transaction-locally before
-- tenant-scoped database work.
-- ============================================================


-- ============================================================
-- SLA POLICIES
-- ============================================================

ALTER TABLE sla_policies ENABLE ROW LEVEL SECURITY;
ALTER TABLE sla_policies FORCE ROW LEVEL SECURITY;

CREATE POLICY sla_policies_tenant_isolation
ON sla_policies
USING (
    organization_id =
    NULLIF(current_setting('app.current_org_id', true), '')::uuid
)
WITH CHECK (
    organization_id =
    NULLIF(current_setting('app.current_org_id', true), '')::uuid
);


-- ============================================================
-- TICKETS
-- ============================================================

ALTER TABLE tickets ENABLE ROW LEVEL SECURITY;
ALTER TABLE tickets FORCE ROW LEVEL SECURITY;

CREATE POLICY tickets_tenant_isolation
ON tickets
USING (
    organization_id =
    NULLIF(current_setting('app.current_org_id', true), '')::uuid
)
WITH CHECK (
    organization_id =
    NULLIF(current_setting('app.current_org_id', true), '')::uuid
);


-- ============================================================
-- TICKET MESSAGES
-- ============================================================

ALTER TABLE ticket_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE ticket_messages FORCE ROW LEVEL SECURITY;

CREATE POLICY ticket_messages_tenant_isolation
ON ticket_messages
USING (
    EXISTS (
        SELECT 1
        FROM tickets t
        WHERE t.id = ticket_messages.ticket_id
          AND t.organization_id =
              NULLIF(
                  current_setting('app.current_org_id', true),
                  ''
              )::uuid
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1
        FROM tickets t
        WHERE t.id = ticket_messages.ticket_id
          AND t.organization_id =
              NULLIF(
                  current_setting('app.current_org_id', true),
                  ''
              )::uuid
    )
);


-- ============================================================
-- TICKET ATTACHMENTS
-- ============================================================

ALTER TABLE ticket_attachments ENABLE ROW LEVEL SECURITY;
ALTER TABLE ticket_attachments FORCE ROW LEVEL SECURITY;

CREATE POLICY ticket_attachments_tenant_isolation
ON ticket_attachments
USING (
    EXISTS (
        SELECT 1
        FROM ticket_messages tm
        JOIN tickets t
          ON t.id = tm.ticket_id
        WHERE tm.id = ticket_attachments.ticket_message_id
          AND t.organization_id =
              NULLIF(
                  current_setting('app.current_org_id', true),
                  ''
              )::uuid
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1
        FROM ticket_messages tm
        JOIN tickets t
          ON t.id = tm.ticket_id
        WHERE tm.id = ticket_attachments.ticket_message_id
          AND t.organization_id =
              NULLIF(
                  current_setting('app.current_org_id', true),
                  ''
              )::uuid
    )
);


-- ============================================================
-- TICKET EVENTS
-- ============================================================

ALTER TABLE ticket_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE ticket_events FORCE ROW LEVEL SECURITY;

CREATE POLICY ticket_events_tenant_isolation
ON ticket_events
USING (
    EXISTS (
        SELECT 1
        FROM tickets t
        WHERE t.id = ticket_events.ticket_id
          AND t.organization_id =
              NULLIF(
                  current_setting('app.current_org_id', true),
                  ''
              )::uuid
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1
        FROM tickets t
        WHERE t.id = ticket_events.ticket_id
          AND t.organization_id =
              NULLIF(
                  current_setting('app.current_org_id', true),
                  ''
              )::uuid
    )
);


-- ============================================================
-- TAGS
-- ============================================================

ALTER TABLE tags ENABLE ROW LEVEL SECURITY;
ALTER TABLE tags FORCE ROW LEVEL SECURITY;

CREATE POLICY tags_tenant_isolation
ON tags
USING (
    organization_id =
    NULLIF(current_setting('app.current_org_id', true), '')::uuid
)
WITH CHECK (
    organization_id =
    NULLIF(current_setting('app.current_org_id', true), '')::uuid
);


-- ============================================================
-- TICKET TAGS
--
-- Both sides must belong to the current organization.
-- ============================================================

ALTER TABLE ticket_tags ENABLE ROW LEVEL SECURITY;
ALTER TABLE ticket_tags FORCE ROW LEVEL SECURITY;

CREATE POLICY ticket_tags_tenant_isolation
ON ticket_tags
USING (
    EXISTS (
        SELECT 1
        FROM tickets t
        JOIN tags tg
          ON tg.id = ticket_tags.tag_id
        WHERE t.id = ticket_tags.ticket_id
          AND t.organization_id =
              NULLIF(
                  current_setting('app.current_org_id', true),
                  ''
              )::uuid
          AND tg.organization_id =
              NULLIF(
                  current_setting('app.current_org_id', true),
                  ''
              )::uuid
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1
        FROM tickets t
        JOIN tags tg
          ON tg.id = ticket_tags.tag_id
        WHERE t.id = ticket_tags.ticket_id
          AND t.organization_id =
              NULLIF(
                  current_setting('app.current_org_id', true),
                  ''
              )::uuid
          AND tg.organization_id =
              NULLIF(
                  current_setting('app.current_org_id', true),
                  ''
              )::uuid
    )
);


-- ============================================================
-- KNOWLEDGE BASE DOCUMENTS
-- ============================================================

ALTER TABLE knowledge_base_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE knowledge_base_documents FORCE ROW LEVEL SECURITY;

CREATE POLICY knowledge_base_documents_tenant_isolation
ON knowledge_base_documents
USING (
    organization_id =
    NULLIF(current_setting('app.current_org_id', true), '')::uuid
)
WITH CHECK (
    organization_id =
    NULLIF(current_setting('app.current_org_id', true), '')::uuid
);


-- ============================================================
-- KNOWLEDGE BASE CHUNKS
--
-- Require both the chunk organization and its parent document
-- to belong to the current tenant.
-- ============================================================

ALTER TABLE kb_chunks ENABLE ROW LEVEL SECURITY;
ALTER TABLE kb_chunks FORCE ROW LEVEL SECURITY;

CREATE POLICY kb_chunks_tenant_isolation
ON kb_chunks
USING (
    organization_id =
        NULLIF(
            current_setting('app.current_org_id', true),
            ''
        )::uuid
    AND EXISTS (
        SELECT 1
        FROM knowledge_base_documents d
        WHERE d.id = kb_chunks.document_id
          AND d.organization_id =
              NULLIF(
                  current_setting('app.current_org_id', true),
                  ''
              )::uuid
    )
)
WITH CHECK (
    organization_id =
        NULLIF(
            current_setting('app.current_org_id', true),
            ''
        )::uuid
    AND EXISTS (
        SELECT 1
        FROM knowledge_base_documents d
        WHERE d.id = kb_chunks.document_id
          AND d.organization_id =
              NULLIF(
                  current_setting('app.current_org_id', true),
                  ''
              )::uuid
    )
);


-- ============================================================
-- AI CLASSIFICATIONS
-- ============================================================

ALTER TABLE ai_classifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_classifications FORCE ROW LEVEL SECURITY;

CREATE POLICY ai_classifications_tenant_isolation
ON ai_classifications
USING (
    EXISTS (
        SELECT 1
        FROM tickets t
        WHERE t.id = ai_classifications.ticket_id
          AND t.organization_id =
              NULLIF(
                  current_setting('app.current_org_id', true),
                  ''
              )::uuid
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1
        FROM tickets t
        WHERE t.id = ai_classifications.ticket_id
          AND t.organization_id =
              NULLIF(
                  current_setting('app.current_org_id', true),
                  ''
              )::uuid
    )
);


-- ============================================================
-- APPLICATION AUDIT LOG
-- ============================================================

ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs FORCE ROW LEVEL SECURITY;

CREATE POLICY audit_logs_tenant_isolation
ON audit_logs
USING (
    organization_id =
    NULLIF(current_setting('app.current_org_id', true), '')::uuid
)
WITH CHECK (
    organization_id =
    NULLIF(current_setting('app.current_org_id', true), '')::uuid
);


-- ============================================================
-- ANALYTICS AI INSIGHTS
-- ============================================================

ALTER TABLE analytics_ai_insights ENABLE ROW LEVEL SECURITY;
ALTER TABLE analytics_ai_insights FORCE ROW LEVEL SECURITY;

CREATE POLICY analytics_ai_insights_tenant_isolation
ON analytics_ai_insights
USING (
    organization_id =
    NULLIF(current_setting('app.current_org_id', true), '')::uuid
)
WITH CHECK (
    organization_id =
    NULLIF(current_setting('app.current_org_id', true), '')::uuid
);
