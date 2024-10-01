package maweituo.tests.containers

import cats.effect.kernel.{Resource, Sync}
import cats.syntax.all.*
import com.dimafeng.testcontainers.{Container, MinIOContainer, RedisContainer}
import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import io.minio.MinioAsyncClient
import org.testcontainers.utility.DockerImageName

private def makeContainerResource[F[_], C <: Container](container: F[C])(using F: Sync[F]): Resource[F, C] =
  Resource.make(container.flatTap {
    container =>
      F.blocking(container.start())
  })(c => F.blocking(c.stop()))

private val redisContainerDef = RedisContainer.Def(
  dockerImageName = DockerImageName.parse("redis:6.2-alpine")
)

private val minioContainerDef = MinIOContainer.Def(
  dockerImageName = DockerImageName.parse("minio/minio:latest"),
  userName = "minioadmin",
  password = "minioadmin"
)

def makeRedisContainerResource[F[_]: Sync]: Resource[F, RedisContainer] =
  makeContainerResource(Sync[F].blocking(redisContainerDef.start()))

def makeRedisResource[F[_]: Sync: MkRedis]: Resource[F, RedisCommands[F, String, String]] =
  makeRedisContainerResource.flatMap(cont =>
    Redis[F].utf8(cont.redisUri)
  )

def makeMinioContainerResource[F[_]: Sync]: Resource[F, MinIOContainer] =
  makeContainerResource(Sync[F].blocking(minioContainerDef.start()))

def makeMinioResource[F[_]: Sync]: Resource[F, MinioAsyncClient] =
  makeMinioContainerResource.map(cont =>
    MinioAsyncClient
      .builder()
      .endpoint(cont.s3URL)
      .credentials(cont.userName, cont.password)
      .build()
  )
