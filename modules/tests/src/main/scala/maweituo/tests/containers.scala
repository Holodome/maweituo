package maweituo
package tests
package containers

import scala.concurrent.ExecutionContext
import scala.io.Source

import maweituo.infrastructure.ObjectStorage
import maweituo.infrastructure.minio.{MinioConnection, MinioObjectStorage}

import com.dimafeng.testcontainers.{Container, MinIOContainer, PostgreSQLContainer, RedisContainer}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import doobie.Transactor
import doobie.syntax.all.*
import doobie.util.ExecutionContexts
import doobie.util.fragment.Fragment
import doobie.util.log.{LogEvent, LogHandler}
import io.minio.MinioAsyncClient
import org.testcontainers.utility.DockerImageName
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.*

private def makeContainerResource[C <: Container](container: IO[C]): Resource[IO, C] =
  Resource.make(container.flatTap {
    container =>
      IO.blocking(container.start())
  })(c => IO.blocking(c.stop()))

private val redisContainerDef = RedisContainer.Def(
  dockerImageName = DockerImageName.parse("redis:6.2-alpine")
)

def makeRedisContainerResource: Resource[IO, RedisContainer] =
  makeContainerResource(IO.blocking(redisContainerDef.start()))

final class PartiallyAppliedRedis(cont: RedisContainer):
  def apply()(using LoggerFactory[IO]): Resource[IO, RedisCommands[IO, String, String]] =
    given Logger[IO]  = LoggerFactory[IO].getLogger
    given MkRedis[IO] = MkRedis.forAsync[IO](using Async[IO], dev.profunktor.redis4cats.log4cats.log4CatsInstance[IO])
    Redis[IO].utf8(cont.redisUri)

def makeRedisResource: Resource[IO, PartiallyAppliedRedis] =
  makeRedisContainerResource.map(cont => PartiallyAppliedRedis(cont))

private val minioContainerDef = MinIOContainer.Def(
  dockerImageName = DockerImageName.parse("minio/minio:RELEASE.2023-09-04T19-57-37Z"),
  userName = "minioadmin",
  password = "minioadmin"
)

def makeMinioContainerResource: Resource[IO, MinIOContainer] =
  makeContainerResource(IO.blocking(minioContainerDef.start()))

final class PartiallyAppliedMinio(cont: MinIOContainer):
  def apply()(using LoggerFactory[IO]): Resource[IO, ObjectStorage[IO]] =
    Resource.eval(MinioObjectStorage.make[IO](
      MinioConnection(
        cont.s3URL,
        MinioAsyncClient
          .builder()
          .endpoint(cont.s3URL)
          .credentials(cont.userName, cont.password)
          .build()
      ),
      "maweituo"
    ))

def makeMinioResource: Resource[IO, PartiallyAppliedMinio] =
  makeMinioContainerResource.map(x => PartiallyAppliedMinio(x))

private def postgresContainerDef = PostgreSQLContainer.Def(
  dockerImageName = DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"),
  username = "maweituo",
  password = "maweituo",
  databaseName = "maweituo"
)

def makePostgresContainerResource: Resource[IO, PostgreSQLContainer] =
  makeContainerResource(IO.blocking(postgresContainerDef.start()))

final class PartiallyAppliedPostgres(ec: ExecutionContext, dataSource: HikariDataSource):
  def apply()(using LoggerFactory[IO]): Resource[IO, Transactor[IO]] =
    given Logger[IO] = LoggerFactory[IO].getLogger
    val logHandler = Some(new LogHandler[IO]:
      def run(logEvent: LogEvent): IO[Unit] =
        if logEvent.sql.contains("create table if not exists users") && false then IO.unit
        else info"sql ${logEvent.sql}"
    )
    Resource.pure(Transactor.fromDataSource[IO](dataSource, ec, logHandler))

def makePostgresResource: Resource[IO, PartiallyAppliedPostgres] =
  makePostgresContainerResource.flatMap { cont =>
    val hikariConfig =
      val config = new HikariConfig()
      config.setDriverClassName("org.postgresql.Driver")
      config.setJdbcUrl(cont.jdbcUrl)
      config.setUsername(cont.username)
      config.setPassword(cont.password)
      config
    val ec =
      for
        _          <- IO.delay(hikariConfig.validate()).toResource
        connectEC  <- ExecutionContexts.fixedThreadPool[IO](hikariConfig.getMaximumPoolSize)
        dataSource <- Resource.fromAutoCloseable(IO.delay(new HikariDataSource(hikariConfig)))
      yield connectEC -> dataSource
    ec.evalTap { (ec, dataSource) =>
      val xa = Transactor.fromDataSource[IO](dataSource, ec)
      Fragment.const(Source.fromFile("deploy/init.sql").mkString)
        .update.run.transact(xa).void
    }
  }.map((ec, dataSource) => PartiallyAppliedPostgres(ec, dataSource))
