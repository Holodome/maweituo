package com.holodome.tests.services

import cats.effect.IO
import cats.syntax.all._
import com.holodome.auth.JwtTokens
import com.holodome.domain.errors.NoUserFound
import com.holodome.domain.repositories._
import com.holodome.domain.users.UserId
import com.holodome.infrastructure.EphemeralDict
import com.holodome.infrastructure.inmemory.InMemoryEphemeralDict
import com.holodome.interpreters._
import com.holodome.tests.generators._
import com.holodome.tests.repositories._
import com.holodome.utils._
import dev.profunktor.auth.jwt.JwtToken
import org.mockito.MockitoSugar
import org.mockito.cats.MockitoCats
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object AuthServiceSuite extends SimpleIOSuite with Checkers with MockitoSugar with MockitoCats {
  implicit val logger: Logger[IO] = NoOpLogger[IO]

  private val iam = IAMServiceInterpreter.make(
    mock[AdvertisementRepository[IO]],
    mock[ChatRepository[IO]],
    mock[AdImageRepository[IO]]
  )
  private def jwtDict: EphemeralDict[IO, JwtToken, UserId]         = InMemoryEphemeralDict.make
  private def authedUsersDict: EphemeralDict[IO, UserId, JwtToken] = InMemoryEphemeralDict.make

  test("login on invalid user fails") {
    val gen = for {
      name     <- usernameGen
      password <- passwordGen
    } yield name -> password
    forall(gen) { case (name, password) =>
      val usersRepo = new InMemoryUserRepository[IO]
      val tokens    = mock[JwtTokens[IO]]
      val auth      = AuthServiceInterpreter.make[IO](usersRepo, authedUsersDict, jwtDict, tokens)
      for {
        x <- auth
          .login(name, password)
          .as(None)
          .recoverWith { case NoUserFound(name) =>
            Some(name).pure[IO]
          }
      } yield expect.all(x.fold(false)(_ === name))
    }
  }

  test("unauthenticated user is so") {
    forall(nesGen(s => JwtToken(s.value))) { token =>
      val usersRepo = new InMemoryUserRepository[IO]
      val tokens    = mock[JwtTokens[IO]]
      val auth      = AuthServiceInterpreter.make[IO](usersRepo, authedUsersDict, jwtDict, tokens)
      for {
        x <- auth.authed(token).value
      } yield expect.all(x.isEmpty)
    }
  }

  test("login works") {
    forall(registerGen) { reg =>
      val usersRepo   = new InMemoryUserRepository[IO]
      val userService = UserServiceInterpreter.make[IO](usersRepo, iam)
      val tokens = new JwtTokens[IO] {
        override def create(userId: UserId): IO[JwtToken] = JwtToken("token").pure[IO]
      }
      val auth = AuthServiceInterpreter.make[IO](usersRepo, authedUsersDict, jwtDict, tokens)
      for {
        id     <- userService.create(reg)
        (t, _) <- auth.login(reg.name, reg.password)
        x      <- auth.authed(t).value
      } yield expect.all(t.value === "token", x.fold(false)(_.id === id))
    }
  }

  test("logout works") {
    forall(registerGen) { reg =>
      val usersRepo = new InMemoryUserRepository[IO]
      val users     = UserServiceInterpreter.make(usersRepo, iam)
      val tokens = new JwtTokens[IO] {
        override def create(userId: UserId): IO[JwtToken] = JwtToken("token").pure[IO]
      }
      val auth = AuthServiceInterpreter.make[IO](usersRepo, authedUsersDict, jwtDict, tokens)
      for {
        id     <- users.create(reg)
        (t, _) <- auth.login(reg.name, reg.password)
        _      <- auth.logout(id, t)
        x      <- auth.authed(t).value
      } yield expect.all(x.isEmpty)
    }
  }
}
