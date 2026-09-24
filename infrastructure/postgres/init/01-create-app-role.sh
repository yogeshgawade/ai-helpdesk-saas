#!/bin/bash
set -e

: "${APP_DB_PASSWORD:?APP_DB_PASSWORD must be set}"

psql -v ON_ERROR_STOP=1 \
    --username "$POSTGRES_USER" \
    --dbname "$POSTGRES_DB" \
    -v app_db_password="$APP_DB_PASSWORD" <<'SQL'

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_roles
        WHERE rolname = 'helpdesk_app'
    ) THEN
        CREATE ROLE helpdesk_app
            LOGIN
            NOSUPERUSER
            NOCREATEDB
            NOCREATEROLE
            NOINHERIT
            NOREPLICATION
            NOBYPASSRLS;
    END IF;
END
$$;

ALTER ROLE helpdesk_app PASSWORD :'app_db_password';

GRANT CONNECT ON DATABASE helpdesk TO helpdesk_app;

GRANT USAGE ON SCHEMA public TO helpdesk_app;

ALTER DEFAULT PRIVILEGES FOR ROLE helpdesk IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO helpdesk_app;

ALTER DEFAULT PRIVILEGES FOR ROLE helpdesk IN SCHEMA public
GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO helpdesk_app;

SQL

