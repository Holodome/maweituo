package com.holodome.tests.services

import java.util.UUID

import com.holodome.domain.errors.{ InvalidAccess, InvalidUserId }
import com.holodome.domain.services.UserService
import com.holodome.domain.users.UserId
import com.holodome.interpreters.*
import com.holodome.tests.generators.{ registerGen, updateUserGen, userIdGen }
import com.holodome.tests.repositories.*
import com.holodome.tests.repositories.inmemory.InMemoryRepositoryFactory
import com.holodome.tests.repositories.stubs.RepositoryStubFactory

import cats.effect.IO
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object UserServiceSuite extends SimpleIOSuite with Checkers:

  given Logger[IO] = NoOpLogger[IO]

  private def makeTestUsers: UserService[IO] =
    val repo = InMemoryRepositoryFactory.users
    val ads  = RepositoryStubFactory.ads
    val iam  = IAMServiceInterpreter.make[IO](ads, RepositoryStubFactory.chats, RepositoryStubFactory.images)
    UserServiceInterpreter.make(repo, ads, iam)

  test("register user works") {
    val users = makeTestUsers
    forall(registerGen) { register =>
      for
        _ <- users.create(register)
      yield expect.all(true)
    }
  }

  test("register and find work") {
    val users = makeTestUsers
    forall(registerGen) { register =>
      for
        id <- users.create(register)
        u  <- users.get(id)
      yield expect.all(u.id === id)
    }
  }

  test("register and find by name work") {
    val users = makeTestUsers
    forall(registerGen) { register =>
      for
        id <- users.create(register)
        u  <- users.getByName(register.name)
      yield expect.all(u.id === id)
    }
  }

  test("register and delete work") {
    val users = makeTestUsers
    forall(registerGen) { register =>
      for
        id <- users.create(register)
        _  <- users.delete(id, id)
        found <- users
          .get(id)
          .map(Some(_))
          .recoverWith { case InvalidUserId(_) => None.pure[IO] }
      yield expect.all(found.isEmpty)
    }
  }

  test("user delete by other person is forbidden") {
    val users = makeTestUsers
    val gen =
      for
        r     <- registerGen
        other <- registerGen
      yield r -> other
    forall(gen) { case (register, other) =>
      for
        newId   <- users.create(register)
        otherId <- users.create(other)
        x <- users
          .delete(newId, otherId)
          .map(Some(_))
          .recover { case InvalidAccess(_) =>
            None
          }
        _ <- users.get(newId)
      yield expect.all(x.isEmpty)
    }
  }

  test("user update works") {
    val users = makeTestUsers
    val gen =
      for
        r   <- registerGen
        upd <- updateUserGen(UserId(UUID.randomUUID()))
      yield (r, upd)
    forall(gen) { case (register, upd) =>
      for
        newId <- users.create(register)
        newUpd = upd.copy(id = newId)
        prior   <- users.get(newId)
        _       <- users.update(newUpd, newId)
        updated <- users.get(newId)
      yield expect.all(
        newUpd.email.fold(true)(_ === updated.email),
        newUpd.name.fold(true)(_ === updated.name),
        newUpd.password.fold(true)(_ => prior.hashedPassword =!= updated.hashedPassword)
      )
    }
  }

  test("user update by other person is forbidden") {
    val users = makeTestUsers
    val gen =
      for
        r   <- registerGen
        upd <- updateUserGen(UserId(UUID.randomUUID()))
        id  <- userIdGen
      yield (r, upd, id)
    forall(gen) { case (register, upd, id) =>
      for
        newId <- users.create(register)
        newUpd = upd.copy(id = newId)
        prior <- users.get(newId)
        x     <- users.update(newUpd, id).map(Some(_)).recover { case InvalidAccess(_) => None }
        got   <- users.get(newId)
      yield expect.all(
        x.isEmpty,
        got.hashedPassword === prior.hashedPassword,
        got.name === prior.name,
        got.email === prior.email
      )
    }
  }
