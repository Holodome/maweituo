package com.holodome.repositories.cassandra

import cats.effect.Resource
import cats.effect.kernel.{Sync, Async}
import com.holodome.config.types.CassandraConfig
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

  private def makeDbResource[F[_]: Sync, D <: Database[D]](db: => D): Resource[F, D] =
    Resource.make(Sync[F].blocking(db))(db => Sync[F].blocking(db.shutdown()))
}
