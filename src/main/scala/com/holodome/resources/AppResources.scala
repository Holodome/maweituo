package com.holodome.resources

import cats.effect.{Async, Concurrent, Resource}
import cats.syntax.all._
import com.datastax.oss.driver.api.core.CqlSession
import com.holodome.config.types.{AppConfig, CassandraConfig, RedisConfig}
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.effect.MkRedis
import org.typelevel.log4cats.Logger

import java.net.InetSocketAddress

sealed abstract class AppResources[F[_]](
    val redis: RedisCommands[F, String, String],
    val cassandra: CassandraSession[F]
)

object AppResources {
  def make[F[_]: MkRedis: Concurrent: Logger: Async](
      cfg: AppConfig
  ): Resource[F, AppResources[F]] = {
    def checkRedisConnection(redis: RedisCommands[F, String, String]): F[Unit] =
      redis.info flatMap {
        _.get("redis_version").traverse_ { v =>
          Logger[F].info(s"Connected to redis $v")
        }
      }

    def checkCassandraConnection(cassandra: CassandraSession[F]): F[Unit] =
      cql"select release_version from system.local".as[String].select(cassandra).head.compile.last flatMap {
        version => Logger[F].info(s"Connected to cassandra $version")
      }

    def mkRedisResource(c: RedisConfig): Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(c.uri.value).evalTap(checkRedisConnection)

    def mkCassandraResource(c: CassandraConfig): Resource[F, CassandraSession[F]] = {
      val builder = CqlSession
        .builder()
        .addContactPoint(InetSocketAddress.createUnresolved(c.host.toString, c.port.value))
        .withLocalDatacenter(c.datacenter)
        .withKeyspace(c.keyspace)
      CassandraSession.connect(builder).evalTap(checkCassandraConnection)
    }

    (mkRedisResource(cfg.redis), mkCassandraResource(cfg.cassandra))
      .parMapN(new AppResources[F](_, _) {})
  }
}
