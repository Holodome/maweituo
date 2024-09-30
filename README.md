# maweituo

Classified advertisements web application with recommendation system

## Technical stack

* Scala 2.13 with cats-effect
* ScyllaDB as primary database
* Clickhouse as recommendation system database
* minio 
* svelte-kit

## Build & Deploy

Application can only be run using docker-compose (local builds are not supported).

Databases have to be configured prior to application being able to be launched:
```shell
docker-compose -f deploy/it.docker-compose.yml up 
./scripts/prepare-it.sh
docker-compose -f deploy/it.docker-compose.yml down
```

To run whole application:

```shell
./scripts/build-dockers.sh 
docker-compose -f deploy/docker-compose.yml up 
```

For testing it may be benefitial to make test database and bootstrap recommendation system:
```shell
./build-dockers.sh
docker-compose -f deploy/it.docker-compose.yml up -d 
./scripts/prepare-it.sh
./scripts/test-populate.py
docker-compose -f deploy/it.docker-compose.yml down 
grpcurl -plaintext -import-path proto -proto proto/rec.proto -d '{}' 'localhost:11223' com.maweituo.proto.RecommendationService/learn
docker-compose -f deploy/docker-compose.yml up -d 
```
