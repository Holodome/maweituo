package com.holodome.cassandra

import cats.effect.{IO, Resource}
import com.datastax.oss.driver.api.core.CqlSession
import com.holodome.tests.ResourceSuite
import com.ringcentral.cassandra4io.CassandraSession

import java.net.InetSocketAddress

abstract class CassandraSuite extends ResourceSuite {
  override type Res = CassandraSession[IO]

  private val Datacenter = "datacenter1"
  private val Keyspace   = "local"
  private val Host       = "localhost"
  private val Port       = 9042

  override def sharedResource: Resource[IO, Res] =
    CassandraSession.connect(
      CqlSession
        .builder()
        .addContactPoint(InetSocketAddress.createUnresolved(Host, Port))
        .withLocalDatacenter(Datacenter)
        .withKeyspace(Keyspace)
    )
}
