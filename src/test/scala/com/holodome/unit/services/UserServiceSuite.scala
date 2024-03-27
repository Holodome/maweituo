package com.holodome.unit.services

import cats.effect.IO
import cats.syntax.all._
import com.holodome.domain.users.{InvalidAccess, InvalidUserId, UserId}
import com.holodome.repositories.{AdvertisementRepository, ChatRepository, ImageRepository}
import com.holodome.utils.generators.{registerGen, updateUserGen, userIdGen}
import com.holodome.services.{IAMService, UserService}
import com.holodome.utils.repositories.InMemoryUserRepository
import org.mockito.MockitoSugar.mock
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import java.util.UUID

object UserServiceSuite extends SimpleIOSuite with Checkers {
  private val iam = IAMService.make(
    mock[AdvertisementRepository[IO]],
    mock[ChatRepository[IO]],
    mock[ImageRepository[IO]]
  )

  test("register user works") {
    forall(registerGen) { register =>
      val repo = new InMemoryUserRepository[IO]
      val serv = UserService.make(repo, iam)
      for {
        _ <- serv.register(register)
      } yield expect.all(true)
    }
  }

  test("register and find work") {
    forall(registerGen) { register =>
      val repo = new InMemoryUserRepository[IO]
      val serv = UserService.make(repo, iam)
      for {
        id <- serv.register(register)
        u  <- serv.find(id)
      } yield expect.all(u.id === id)
    }
  }

  test("register and find by name work") {
    forall(registerGen) { register =>
      val repo = new InMemoryUserRepository[IO]
      val serv = UserService.make(repo, iam)
      for {
        id <- serv.register(register)
        u  <- serv.findByName(register.name)
      } yield expect.all(u.id === id)
    }
  }

  test("register and delete work") {
    forall(registerGen) { register =>
      val repo = new InMemoryUserRepository[IO]
      val serv = UserService.make(repo, iam)
      for {
        id <- serv.register(register)
        _  <- serv.delete(id, id)
        found <- serv
          .find(id)
          .map(Some(_))
          .recoverWith { case InvalidUserId() => None.pure[IO] }
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
      val serv = UserService.make(repo, iam)
      for {
        newId   <- serv.register(register)
        otherId <- serv.register(other)
        x <- serv
          .delete(newId, otherId)
          .map(Some(_))
          .recover { case InvalidAccess() =>
            None
          }
        _ <- serv.find(newId)
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
      val serv = UserService.make(repo, iam)
      for {
        newId <- serv.register(register)
        newUpd = upd.copy(id = newId)
        prior   <- serv.find(newId)
        _       <- serv.update(newUpd, newId)
        updated <- serv.find(newId)
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
      val serv = UserService.make(repo, iam)
      for {
        newId <- serv.register(register)
        newUpd = upd.copy(id = newId)
        prior <- serv.find(newId)
        x     <- serv.update(newUpd, id).map(Some(_)).recover { case InvalidAccess() => None }
        got   <- serv.find(newId)
      } yield expect.all(
        x.isEmpty,
        got.hashedPassword === prior.hashedPassword,
        got.name === prior.name,
        got.email === prior.email
      )
    }
  }
}
