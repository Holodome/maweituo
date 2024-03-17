package com.holodome

import cats.effect.{ContextShift, ExitCode, IO, IOApp}

import scala.concurrent.ExecutionContext
import com.holodome.config.Config
import com.holodome.Server
import com.holodome.repositories.UsersRepository
import com.holodome.repositories.cassandra.cql.UsersDatabase
import com.outworkers.phantom.connectors.ContactPoints
import com.outworkers.phantom.dsl.CassandraConnection
import com.holodome.repositories.cassandra.CassandraUsersRepository

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val config = Config(1235)

    val connection: CassandraConnection =
      ContactPoints(List("scylla-node1", "scylla-node2", "scylla-node3"))
        .keySpace("catalog")
    val db = new UsersDatabase(connection)
    val cassandraUsers: UsersRepository = new CassandraUsersRepository(db)

    Server.run(config)
  }.map(_ => ExitCode.Success)
}
