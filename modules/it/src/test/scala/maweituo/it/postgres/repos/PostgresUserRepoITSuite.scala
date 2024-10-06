package maweituo.it.postgres.repos

import cats.effect.*
import cats.syntax.all.*

import maweituo.domain.users.UpdateUserRepoRequest
import maweituo.domain.users.repos.UserRepo
import maweituo.postgres.repos.users.PostgresUserRepo
import maweituo.tests.ResourceSuite
import maweituo.tests.generators.{updateUserGen, userGen}
import maweituo.tests.resources.*
import maweituo.tests.utils.given

import doobie.util.transactor.Transactor
import weaver.*
import weaver.scalacheck.Checkers

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
      yield expect.same(u, Some(user))
    }
  }

  usersTest("create and find by name") { users =>
    forall(userGen) { user =>
      for
        _ <- users.create(user)
        u <- users.findByName(user.name).value
      yield expect.same(u, Some(user))
    }
  }

  usersTest("create and find by email") { users =>
    forall(userGen) { user =>
      for
        _ <- users.create(user)
        u <- users.findByEmail(user.email).value
      yield expect.same(u, Some(user))
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
      val upd = UpdateUserRepoRequest.fromReq(upd0, user.salt)
      for
        _ <- users.create(user)
        _ <- users.update(upd)
        u <- users.find(user.id).value
      yield matches(u) { case Some(u) =>
        expect.all(
          upd.email.fold(true)(_ === u.email),
          upd.name.fold(true)(_ === u.name),
          upd.password.fold(true)(_ => user.hashedPassword =!= u.hashedPassword)
        )
      }
    }
  }
