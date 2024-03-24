package com.holodome.repositories.cassandra

import cats.effect.Resource
import cats.effect.kernel.{Async, Sync}
import com.holodome.config.types.CassandraConfig
import com.holodome.ext.phantom.catsInterop.makeDbResource
import com.holodome.repositories.UserRepository
import com.holodome.repositories.cassandra.cql.UsersDatabase
import com.outworkers.phantom.connectors
import com.outworkers.phantom.dsl.Database

sealed abstract class CassandraResources {
  val userDb: UsersDatabase
}

object CassandraResources {
  def make[F[_]: Async](config: CassandraConfig): Resource[F, CassandraResources] = {
    val connector = connectors.ContactPoint.local.keySpace(config.keyspace)
    val userDb    = makeDbResource(new UsersDatabase(connector))

    userDb.map(userDb_ =>
      new CassandraResources {
        override val userDb: UsersDatabase = userDb_
      }
    )
  }

}
