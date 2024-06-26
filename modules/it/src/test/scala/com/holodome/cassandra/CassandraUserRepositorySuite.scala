package com.holodome.cassandra

import cats.Show
import cats.effect.IO
import cats.syntax.all._
import com.holodome.cassandra.repositories.CassandraUserRepository
import com.holodome.domain.errors.NoUserFound
import com.holodome.domain.users.User
import com.holodome.tests.generators.userGen

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
