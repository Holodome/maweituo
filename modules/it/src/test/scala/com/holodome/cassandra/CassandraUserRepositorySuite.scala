package maweituo.cassandra

import cats.Show
import cats.effect.IO
import cats.syntax.all.*
import maweituo.cassandra.repos.CassandraUserRepository
import maweituo.domain.errors.NoUserFound
import maweituo.domain.users.User
import maweituo.tests.generators.userGen

object CassandraUserRepositorySuite extends CassandraSuite:

  given Show[User] = Show.show(_ => "User")

  test("basic operations works") { cassandra =>
    forall(userGen) { user =>
      val repo = CassandraUserRepository.make[IO](cassandra)
      for
        _ <- repo.create(user)
        u <- repo.find(user.id).getOrRaise(NoUserFound(user.name))
        _ <- repo.delete(user.id)
      yield expect.all(
        u.id === user.id,
        u.name === user.name,
        u.email === user.email,
        u.hashedPassword === user.hashedPassword,
        u.salt === user.salt
      )
    }
  }
