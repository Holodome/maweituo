# Unit tests

```shell
sbt tests/test
```

# Integration tests

```shell
docker-compose -f deploy/test-it.docker-compose.yml up -d 
sbt it/test
```

# Running 

```shell
sbt core/docker:publishLocal
sbt recs/docker:publishLocal
```

# Deploy

```shell 
./scripts/build-dockers.sh
docker-compose -f deploy/it.docker-compose.yml up -d 
./scripts/prepare-it.sh
./scripts/test-populate.py
docker-compose -f deploy/it.docker-compose.yml down 
docker-compose -f deploy/docker-compose.yml up -d 
grpcurl -plaintext -import-path proto -proto proto/rec.proto -d '{}' 'localhost:11223' com.maweituo.proto.RecommendationService/learn
```