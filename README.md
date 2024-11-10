# maweituo

Classified advertisements web application with recommendation system

## Technical stack

* Scala 3.4 with cats-effect
* PostgreSQL
* minio 
* svelte-kit

## Testing

```sbt test```

## С балансировкой

```
ab -k -n 1000 -c 10 -L 'http://localhost:80/api/v1/feed/?page_size=100&page=0'
```

```
maweituo[main*](130) % ab -k -n 1000 -c 10 -L 'http://localhost:80/api/v1/feed/?page_size=100&page=0'

Server Software:        nginx/1.27.2
Server Hostname:        localhost
Server Port:            80

Document Path:          /api/v1/feed/?page_size=100&page=0
Document Length:        3984 bytes

Concurrency Level:      10
Time taken for tests:   5.351 seconds
Complete requests:      1000
Failed requests:        0
Keep-Alive requests:    1000
Total transferred:      4213000 bytes
HTML transferred:       3984000 bytes
Requests per second:    186.88 [#/sec] (mean)
Time per request:       53.510 [ms] (mean)
Time per request:       5.351 [ms] (mean, across all concurrent requests)
Transfer rate:          768.87 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    0   0.0      0       0
Processing:    10   53  33.5     43     277
Waiting:       10   53  33.5     43     277
Total:         10   53  33.6     43     277

Percentage of the requests served within a certain time (ms)
  50%     43
  66%     53
  75%     61
  80%     67
  90%     93
  95%    124
  98%    162
  99%    188
 100%    277 (longest request)
```

## Без балансировки

```
ab -k -n 1000 -c 10 -L 'http://localhost:80/api/v1/feed/?page_size=100&page=0'
```

```
Server Software:        nginx/1.27.2
Server Hostname:        localhost
Server Port:            80

Document Path:          /api/v1/feed/?page_size=100&page=0
Document Length:        3984 bytes

Concurrency Level:      10
Time taken for tests:   4.505 seconds
Complete requests:      1000
Failed requests:        0
Keep-Alive requests:    1000
Total transferred:      4213000 bytes
HTML transferred:       3984000 bytes
Requests per second:    221.96 [#/sec] (mean)
Time per request:       45.053 [ms] (mean)
Time per request:       4.505 [ms] (mean, across all concurrent requests)
Transfer rate:          913.21 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    0   0.0      0       0
Processing:    13   41  21.0     38     398
Waiting:       13   41  21.0     38     397
Total:         13   41  21.0     38     398

Percentage of the requests served within a certain time (ms)
  50%     38
  66%     45
  75%     50
  80%     53
  90%     64
  95%     73
  98%     84
  99%    107
 100%    398 (longest request)
```