#!/usr/bin/env bash

set -eox pipefail

# Run Scylla migrations
cqlsh localhost 9042 -u cassandra -p cassandra -f deploy/init.cql

clickhouse client --port 9100 -mn < deploy/init-clickhouse.sql