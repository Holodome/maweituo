#!/usr/bin/env bash

set -x

cd web
docker build . -t maweituo-web
cd -
sbt ";coreHttp/docker:publishLocal;coreConsole/docker:publishLocal;recs/docker:publishLocal"

docker images | grep maweituo
