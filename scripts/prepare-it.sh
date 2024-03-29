#!/usr/bin/env bash

set -eox pipefail

# Run Scylla migrations
cqlsh localhost 9042 -f cql_scripts/init.cql

