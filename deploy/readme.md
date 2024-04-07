# Unit tests

```shell
sbt tests/test
```

# Integration tests

```shell
docker-compose -f deploy/test-docker-compose.yml up -d 
sbt it/test
```

# Running 

```shell
sbt core/docker:publishLocal
sbt recs/docker:publishLocal
```