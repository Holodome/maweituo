#!/usr/bin/env bash

set -x

cd web
docker build . -t maweituo-web
cd -
sbt core/docker:publishLocal
sbt recs/docker:publishLocal

docker images | grep maweituo
