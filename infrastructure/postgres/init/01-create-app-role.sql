DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_roles
        WHERE rolname = 'helpdesk_app'
    ) THEN
        CREATE ROLE helpdesk_app
            LOGIN
            PASSWORD 'helpdesk_app_dev_password'
            NOSUPERUSER
            NOCREATEDB
            NOCREATEROLE
            NOINHERIT
            NOREPLICATION
            NOBYPASSRLS;
    END IF;
END
$$;

GRANT CONNECT ON DATABASE helpdesk TO helpdesk_app;

GRANT USAGE ON SCHEMA public TO helpdesk_app;

ALTER DEFAULT PRIVILEGES FOR ROLE helpdesk IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO helpdesk_app;

ALTER DEFAULT PRIVILEGES FOR ROLE helpdesk IN SCHEMA public
GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO helpdesk_app;
