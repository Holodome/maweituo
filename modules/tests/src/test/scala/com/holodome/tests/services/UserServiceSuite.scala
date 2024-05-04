package com.holodome.tests.services

import cats.effect.IO
import cats.syntax.all._
import com.holodome.domain.errors.{InvalidAccess, InvalidUserId}
import com.holodome.domain.repositories._
import com.holodome.domain.users.UserId
import com.holodome.interpreters._
import com.holodome.tests.generators.{registerGen, updateUserGen, userIdGen}
import com.holodome.tests.repositories._
import org.mockito.MockitoSugar.mock
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import java.util.UUID

object UserServiceSuite extends SimpleIOSuite with Checkers {
  implicit val logger: Logger[IO] = NoOpLogger[IO]

  private val iam = IAMServiceInterpreter.make(
    mock[AdvertisementRepository[IO]],
    mock[ChatRepository[IO]],
    mock[AdImageRepository[IO]]
  )

  test("register user works") {
    forall(registerGen) { register =>
      val repo = new InMemoryUserRepository[IO]
      val serv = UserServiceInterpreter.make(repo, iam)
      for {
        _ <- serv.create(register)
      } yield expect.all(true)
    }
  }

  test("register and find work") {
    forall(registerGen) { register =>
      val repo = new InMemoryUserRepository[IO]
      val serv = UserServiceInterpreter.make(repo, iam)
      for {
        id <- serv.create(register)
        u  <- serv.get(id)
      } yield expect.all(u.id === id)
    }
  }

  test("register and find by name work") {
    forall(registerGen) { register =>
      val repo = new InMemoryUserRepository[IO]
      val serv = UserServiceInterpreter.make(repo, iam)
      for {
        id <- serv.create(register)
        u  <- repo.getByName(register.name)
      } yield expect.all(u.id === id)
    }
  }

  test("register and delete work") {
    forall(registerGen) { register =>
      val repo = new InMemoryUserRepository[IO]
      val serv = UserServiceInterpreter.make(repo, iam)
      for {
        id <- serv.create(register)
        _  <- serv.delete(id, id)
        found <- serv
          .get(id)
          .map(Some(_))
          .recoverWith { case InvalidUserId(_) => None.pure[IO] }
      } yield expect.all(found.isEmpty)
    }
  }

  test("user delete by other person is forbidden") {
    val gen = for {
      r     <- registerGen
      other <- registerGen
    } yield r -> other
    forall(gen) { case (register, other) =>
      val repo = new InMemoryUserRepository[IO]
      val serv = UserServiceInterpreter.make(repo, iam)
      for {
        newId   <- serv.create(register)
        otherId <- serv.create(other)
        x <- serv
          .delete(newId, otherId)
          .map(Some(_))
          .recover { case InvalidAccess(_) =>
            None
          }
        _ <- serv.get(newId)
      } yield expect.all(x.isEmpty)
    }
  }

  test("user update works") {
    val gen = for {
      r   <- registerGen
      upd <- updateUserGen(UserId(UUID.randomUUID()))
    } yield (r, upd)
    forall(gen) { case (register, upd) =>
      val repo = new InMemoryUserRepository[IO]
      val serv = UserServiceInterpreter.make(repo, iam)
      for {
        newId <- serv.create(register)
        newUpd = upd.copy(id = newId)
        prior   <- serv.get(newId)
        _       <- serv.update(newUpd, newId)
        updated <- serv.get(newId)
      } yield expect.all(
        newUpd.email.fold(true)(_ === updated.email),
        newUpd.name.fold(true)(_ === updated.name),
        newUpd.password.fold(true)(_ => prior.hashedPassword =!= updated.hashedPassword)
      )
    }
  }

  test("user update by other person is forbidden") {
    val gen = for {
      r   <- registerGen
      upd <- updateUserGen(UserId(UUID.randomUUID()))
      id  <- userIdGen
    } yield (r, upd, id)
    forall(gen) { case (register, upd, id) =>
      val repo = new InMemoryUserRepository[IO]
      val serv = UserServiceInterpreter.make(repo, iam)
      for {
        newId <- serv.create(register)
        newUpd = upd.copy(id = newId)
        prior <- serv.get(newId)
        x     <- serv.update(newUpd, id).map(Some(_)).recover { case InvalidAccess(_) => None }
        got   <- serv.get(newId)
      } yield expect.all(
        x.isEmpty,
        got.hashedPassword === prior.hashedPassword,
        got.name === prior.name,
        got.email === prior.email
      )
    }
  }
}
