package com.holodome.resources

import cats.syntax.all._
import cats.effect.{Async, Resource}
import com.datastax.oss.driver.api.core.CqlSession
import com.holodome.config.types.CassandraConfig
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext
import org.typelevel.log4cats.Logger

import java.net.InetSocketAddress

trait MkCassandraClient[F[_]] {
  def newClient(c: CassandraConfig): Resource[F, CassandraSession[F]]
}

object MkCassandraClient {
  def apply[F[_]: MkCassandraClient]: MkCassandraClient[F] = implicitly

  implicit def forAsyncLogger[F[_]: Async: Logger]: MkCassandraClient[F] =
    new MkCassandraClient[F] {
      def checkCassandraConnection(cassandra: CassandraSession[F]): F[Unit] =
        cql"select release_version from system.local"
          .as[String]
          .select(cassandra)
          .head
          .compile
          .last flatMap { version =>
          Logger[F].info(s"Connected to cassandra $version")
        }

      override def newClient(c: CassandraConfig): Resource[F, CassandraSession[F]] = {
        val builder = CqlSession
          .builder()
          .addContactPoint(InetSocketAddress.createUnresolved(c.host.toString, c.port.value))
          .withLocalDatacenter(c.datacenter.value)
          .withKeyspace(c.keyspace)
        CassandraSession.connect(builder).evalTap(checkCassandraConnection)
      }
    }
}
