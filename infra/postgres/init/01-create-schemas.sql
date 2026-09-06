-- Local dev bootstrap: create the per-service schemas so each service's
-- Flyway migrations can run against a schema that already exists, even
-- before Flyway's own "CREATE SCHEMA IF NOT EXISTS" migration executes.
-- This script only runs once, the first time the postgres data volume is
-- initialized (see the official postgres image's /docker-entrypoint-initdb.d
-- behavior).
CREATE SCHEMA IF NOT EXISTS ticketing;
CREATE SCHEMA IF NOT EXISTS notification;
