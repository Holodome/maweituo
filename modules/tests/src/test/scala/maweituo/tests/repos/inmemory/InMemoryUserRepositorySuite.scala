package maweituo
package tests
package repos
package inmemory

import maweituo.domain.all.*

object InMemoryUserRepoSuite extends MaweituoSimpleSuite:

  private def repo = InMemoryRepoFactory.users[IO]

  unitTest("create and find") {
    val users = repo
    forall(userGen) { user =>
      for
        _ <- users.create(user)
        u <- users.find(user.id).value
      yield expect.same(u, Some(user))
    }
  }

  unitTest("create and find by name") {
    val users = repo
    forall(userGen) { user =>
      for
        _ <- users.create(user)
        u <- users.findByName(user.name).value
      yield expect.same(u, Some(user))
    }
  }

  unitTest("create and find by email") {
    val users = repo
    forall(userGen) { user =>
      for
        _ <- users.create(user)
        u <- users.findByEmail(user.email).value
      yield expect.same(u, Some(user))
    }
  }

  unitTest("delete") {
    val users = repo
    forall(userGen) { user =>
      for
        _ <- users.create(user)
        _ <- users.delete(user.id)
        x <- users.find(user.id).value
      yield expect.same(None, x)
    }
  }

  unitTest("update") {
    val users = repo
    val gen =
      for
        u   <- userGen
        upd <- updateUserGen(u.id)
      yield u -> upd
    forall(gen) { (user, upd0) =>
      for
        upd <- UpdateUserRepoRequest.fromReq(upd0, user.salt)
        _   <- users.create(user)
        _   <- users.update(upd)
        u   <- users.find(user.id).value
      yield matches(u) { case Some(u) =>
        expect.all(
          upd.email.fold(true)(_ === u.email),
          upd.name.fold(true)(_ === u.name),
          upd.password.fold(true)(_ => user.hashedPassword =!= u.hashedPassword)
        )
      }
    }
  }
