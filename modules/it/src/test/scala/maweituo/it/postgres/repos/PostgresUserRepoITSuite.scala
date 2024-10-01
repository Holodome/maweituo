package maweituo.it.postgres.repos

import cats.effect.*
import cats.syntax.all.*

import maweituo.domain.users.UpdateUserInternal
import maweituo.postgres.repos.users.PostgresUserRepo
import maweituo.tests.containers.*
import maweituo.tests.generators.{updateUserGen, userGen}
import maweituo.tests.utils.given
import maweituo.tests.{ResourceSuite, WeaverLogAdapter}

import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.*
import weaver.scalacheck.Checkers

object PostgresUserRepoITSuite extends ResourceSuite:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] =
    given Logger[IO] = NoOpLogger[IO]
    makePostgresResource[IO]

  test("create and find") { (postgres, log) =>
    given Logger[IO] = new WeaverLogAdapter[IO](log)
    val users        = PostgresUserRepo.make(postgres)
    forall(userGen) { user =>
      for
        _ <- users.create(user)
        u <- users.find(user.id).value
      yield expect.same(u, Some(user))
    }
  }

  test("create and find by name") { (postgres, log) =>
    given Logger[IO] = new WeaverLogAdapter[IO](log)
    val users        = PostgresUserRepo.make(postgres)
    forall(userGen) { user =>
      for
        _ <- users.create(user)
        u <- users.findByName(user.name).value
      yield expect.same(u, Some(user))
    }
  }

  test("create and find by email") { (postgres, log) =>
    given Logger[IO] = new WeaverLogAdapter[IO](log)
    forall(userGen) { user =>
      val users = PostgresUserRepo.make(postgres)
      for
        _ <- users.create(user)
        u <- users.findByEmail(user.email).value
      yield expect.same(u, Some(user))
    }
  }

  test("delete") { (postgres, log) =>
    given Logger[IO] = new WeaverLogAdapter[IO](log)
    val users        = PostgresUserRepo.make(postgres)
    forall(userGen) { user =>
      for
        _ <- users.create(user)
        _ <- users.delete(user.id)
        x <- users.find(user.id).value
      yield expect.same(None, x)
    }
  }

  test("update") { (postgres, log) =>
    given Logger[IO] = new WeaverLogAdapter[IO](log)
    val gen =
      for
        u   <- userGen
        upd <- updateUserGen(u.id)
      yield u -> upd
    val users = PostgresUserRepo.make(postgres)
    forall(gen) { (user, upd0) =>
      val upd = UpdateUserInternal.fromReq(upd0, user.salt)
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
