package com.holodome.cassandra

import cats.syntax.all._
import cats.effect.{IO, Resource}
import cats.Show
import com.datastax.oss.driver.api.core.CqlSession
import com.holodome.ResourceSuite
import com.holodome.domain.users.{NoUserFound, User}
import com.holodome.generators.userGen
import com.holodome.repositories.cassandra.CassandraUserRepository
import com.ringcentral.cassandra4io.CassandraSession

import java.net.InetSocketAddress

object CassandraUserRepositorySuite extends CassandraSuite {

  private implicit val userShow: Show[User] = Show.show(_ => "User")

  test("basic operations works") { cassandra =>
    forall(userGen) { user =>
      val repo = CassandraUserRepository.make[IO](cassandra)
      for {
        _ <- repo.create(user)
        u <- repo.find(user.id).getOrRaise(NoUserFound(user.name))
        _ <- repo.delete(user.id)
      } yield expect.all(
        u.id === user.id,
        u.name === user.name,
        u.email === user.email,
        u.hashedPassword === user.hashedPassword,
        u.salt === user.salt
      )
    }
  }
}
