package maweituo
package tests
package it
package postgres
package repos

import maweituo.domain.all.*
import maweituo.postgres.repos.all.*
import maweituo.tests.resources.*

import doobie.util.transactor.Transactor
import weaver.GlobalRead

class PostgresUserRepoITSuite(global: GlobalRead) extends ResourceSuite:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] =
    global.postgres

  private def usersTest(name: String)(fn: UserRepo[IO] => F[Expectations]) =
    itTest(name) { postgres =>
      val ads = PostgresUserRepo.make(postgres)
      fn(ads)
    }

  usersTest("create and find") { users =>
    forall(userGen) { user =>
      for
        _ <- users.create(user)
        u <- users.find(user.id).value
      yield expect.same(Some(user), u)
    }
  }

  usersTest("create and find by name") { users =>
    forall(userGen) { user =>
      for
        _ <- users.create(user)
        u <- users.findByName(user.name).value
      yield expect.same(Some(user), u)
    }
  }

  usersTest("create and find by email") { users =>
    forall(userGen) { user =>
      for
        _ <- users.create(user)
        u <- users.findByEmail(user.email).value
      yield expect.same(Some(user), u)
    }
  }

  usersTest("delete") { users =>
    forall(userGen) { user =>
      for
        _ <- users.create(user)
        _ <- users.delete(user.id)
        x <- users.find(user.id).value
      yield expect.same(None, x)
    }
  }

  usersTest("update") { users =>
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
