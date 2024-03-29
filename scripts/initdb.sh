#!/usr/bin/env bash

set -eox pipefail

cqlsh localhost 9042 -f cql_scripts/init.cql