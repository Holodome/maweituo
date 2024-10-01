package maweituo.tests.containers

import cats.effect.kernel.{Async, Resource, Sync}
import cats.syntax.all.*
import com.dimafeng.testcontainers.{Container, MinIOContainer, PostgreSQLContainer, RedisContainer}
import com.zaxxer.hikari.HikariConfig
import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import doobie.Transactor
import doobie.hikari.HikariTransactor
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

def makeRedisContainerResource[F[_]: Sync]: Resource[F, RedisContainer] =
  makeContainerResource(Sync[F].blocking(redisContainerDef.start()))

def makeRedisResource[F[_]: Sync: MkRedis]: Resource[F, RedisCommands[F, String, String]] =
  makeRedisContainerResource
    .flatMap(cont =>
      Redis[F].utf8(cont.redisUri)
    )

private val minioContainerDef = MinIOContainer.Def(
  dockerImageName = DockerImageName.parse("minio/minio:RELEASE.2023-09-04T19-57-37Z"),
  userName = "minioadmin",
  password = "minioadmin"
)

def makeMinioContainerResource[F[_]: Sync]: Resource[F, MinIOContainer] =
  makeContainerResource(Sync[F].blocking(minioContainerDef.start()))

def makeMinioResource[F[_]: Sync]: Resource[F, MinioAsyncClient] =
  makeMinioContainerResource
    .flatMap(cont =>
      Resource.make {
        Sync[F].blocking(
          MinioAsyncClient
            .builder()
            .endpoint(cont.s3URL)
            .credentials(cont.userName, cont.password)
            .build()
        )
      } { _ => Sync[F].unit }
    )

private def postgresContainerDef = PostgreSQLContainer.Def(
  dockerImageName = DockerImageName.parse("postgres:17.0"),
  username = "maweituo",
  password = "maweituo",
  databaseName = "maweituo"
)

def makePostgresContainerResource[F[_]: Sync]: Resource[F, PostgreSQLContainer] =
  makeContainerResource(Sync[F].blocking(postgresContainerDef.start()))

def makePostgresResource[F[_]: Async]: Resource[F, Transactor[F]] =
  makePostgresContainerResource.flatMap { cont =>
    for
      hikariConfig <- Resource.pure {
        val config = new HikariConfig()
        config.setDriverClassName("org.postgresql.Driver")
        config.setJdbcUrl(cont.jdbcUrl)
        config.setUsername(cont.username)
        config.setPassword(cont.password)
        config
      }
      xa <- HikariTransactor.fromHikariConfig[F](hikariConfig)
    yield xa
  }
