#!/bin/sh
# Runs once, when the PostgreSQL volume is created: the application user and its two databases.
# The schemas inside them (fsn_identity, fsn_finance) are created by Flyway when the services start.
set -eu
psql -v ON_ERROR_STOP=1 -v app_password="${FSN_DB_PASSWORD}" --username "$POSTGRES_USER" --dbname postgres <<'SQL'
CREATE ROLE fsn_user LOGIN PASSWORD :'app_password';
CREATE DATABASE finsphere_auth OWNER fsn_user;
CREATE DATABASE finsphere_chit_finance OWNER fsn_user;
SQL
