package com.holodome.tests.repositories.inmemory

import com.holodome.domain.users.UpdateUserInternal
import com.holodome.tests.generators.*
import com.holodome.tests.repositories.*
import com.holodome.tests.utils.given

import cats.effect.IO
import cats.syntax.all.*
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object InMemoryUserRepositorySuite extends SimpleIOSuite with Checkers:
  
  private def repo = InMemoryRepositoryFactory.users[IO]

  test("create and find") {
    val users = repo
    forall(userGen) { user =>
      for
        _ <- users.create(user)
        u <- users.find(user.id).value
      yield expect.same(u, Some(user))
    }
  }

  test("create and find by name") {
    val users = repo
    forall(userGen) { user =>
      for
        _ <- users.create(user)
        u <- users.findByName(user.name).value
      yield expect.same(u, Some(user))
    }
  }

  test("create and find by email") {
    val users = repo
    forall(userGen) { user =>
      for
        _ <- users.create(user)
        u <- users.findByEmail(user.email).value
      yield expect.same(u, Some(user))
    }
  }

  test("delete") {
    val users = repo
    forall(userGen) { user =>
      for
        _ <- users.create(user)
        _ <- users.delete(user.id)
        x <- users.find(user.id).value
      yield expect.same(None, x)
    }
  }

  test("update") {
    val users = repo
    val gen =
      for
        u   <- userGen
        upd <- updateUserGen(u.id)
      yield u -> upd
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
