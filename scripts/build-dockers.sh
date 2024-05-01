#!/usr/bin/env bash

set -x

sbt core/docker:publishLocal
sbt recs/docker:publishLocal

docker images | grep maweituo
