#!/bin/bash
set -ex

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
create extension pgvector;
EOSQL