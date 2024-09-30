package com.holodome.tests.services

import java.util.UUID

import com.holodome.domain.errors.*
import com.holodome.domain.services.IAMService
import com.holodome.domain.users.UserId
import com.holodome.domain.users.services.UserService
import com.holodome.interpreters.*
import com.holodome.interpreters.users.*
import com.holodome.tests.generators.{ registerGen, updateUserGen, userIdGen }
import com.holodome.tests.repos.*
import com.holodome.tests.repos.inmemory.InMemoryRepositoryFactory

import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object UserServiceSuite extends SimpleIOSuite with Checkers:

  given Logger[IO]     = NoOpLogger[IO]
  given IAMService[IO] = makeIAMService

  private def makeTestUsers: UserService[IO] =
    val repo = InMemoryRepositoryFactory.users
    UserServiceInterpreter.make(repo)

  test("register user works") {
    val users = makeTestUsers
    forall(registerGen) { register =>
      for
        _ <- users.create(register)
      yield expect.all(true)
    }
  }

  test("can't register user with same email") {
    val users = makeTestUsers
    val gen =
      for
        r1 <- registerGen
        r2 <- registerGen
      yield r1 -> r2
    forall(gen) { (r1, r2_0) =>
      val r2 = r2_0.copy(email = r1.email)
      for
        _ <- users.create(r1)
        x <- users.create(r2).attempt
      yield expect.same(Left(UserEmailInUse(r1.email)), x)
    }
  }

  test("can't register user with same name") {
    val users = makeTestUsers
    val gen =
      for
        r1 <- registerGen
        r2 <- registerGen
      yield r1 -> r2
    forall(gen) { (r1, r2_0) =>
      val r2 = r2_0.copy(name = r1.name)
      for
        _ <- users.create(r1)
        x <- users.create(r2).attempt
      yield expect.same(Left(UserNameInUse(r1.name)), x)
    }
  }

  test("register and find work") {
    val users = makeTestUsers
    forall(registerGen) { register =>
      for
        id <- users.create(register)
        u  <- users.get(id)
      yield expect.same(id, u.id)
    }
  }

  test("register and find by name work") {
    val users = makeTestUsers
    forall(registerGen) { register =>
      for
        id <- users.create(register)
        u  <- users.getByName(register.name)
      yield expect.same(id, u.id)
    }
  }

  test("register and delete work") {
    val users = makeTestUsers
    forall(registerGen) { register =>
      for
        id    <- users.create(register)
        _     <- users.delete(id, id)
        found <- users.get(id).attempt
      yield expect.same(Left(InvalidUserId(id)), found)
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
        x       <- users.delete(newId, otherId).attempt
        u       <- users.get(newId)
      yield NonEmptyList
        .of(
          expect.same(Left(InvalidAccess(otherId)), x),
          expect.same(newId, u.id),
          expect.same(register.name, u.name),
          expect.same(register.email, u.email)
        ).reduce
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
        x     <- users.update(newUpd, id).attempt
        got   <- users.get(newId)
      yield NonEmptyList.of(
        expect.same(Left(InvalidAccess(id)), x),
        expect.same(prior.hashedPassword, got.hashedPassword),
        expect.same(prior.name, got.name),
        expect.same(prior.email, got.email)
      ).reduce
    }
  }
