#!/usr/bin/env bash

set -x

( cd web;  docker build . -t maweituo-web ) &
( sbt ";core/docker:publishLocal" ) &

wait 
docker images | grep maweituo
