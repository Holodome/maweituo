package com.holodome.tests.services

import com.holodome.auth.JwtTokens
import com.holodome.domain.errors.NoUserFound
import com.holodome.domain.services.{AuthService, UserService}
import com.holodome.domain.users.UserId
import com.holodome.infrastructure.EphemeralDict
import com.holodome.infrastructure.inmemory.InMemoryEphemeralDict
import com.holodome.interpreters.*
import com.holodome.tests.generators.*
import com.holodome.tests.repositories.*
import com.holodome.tests.repositories.inmemory.InMemoryRepositoryFactory
import com.holodome.tests.repositories.stubs.RepositoryStubFactory
import com.holodome.utils.given

import cats.effect.IO
import cats.syntax.all.*
import dev.profunktor.auth.jwt.JwtToken
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object AuthServiceSuite extends SimpleIOSuite with Checkers:
  given Logger[IO] = NoOpLogger[IO]

  private def jwtDict: EphemeralDict[IO, JwtToken, UserId]         = InMemoryEphemeralDict.make
  private def authedUsersDict: EphemeralDict[IO, UserId, JwtToken] = InMemoryEphemeralDict.make

  def makeTestUsersAuth: (UserService[F], AuthService[F]) =
    val tokens   = new TestJwtTokens
    val userRepo = InMemoryRepositoryFactory.users
    val adRepo   = InMemoryRepositoryFactory.ads
    val iam      = IAMServiceInterpreter.make(adRepo, RepositoryStubFactory.chats, RepositoryStubFactory.images)
    val users    = UserServiceInterpreter.make[IO](userRepo, adRepo, iam)
    val auth     = AuthServiceInterpreter.make[IO](userRepo, authedUsersDict, jwtDict, tokens)
    (users, auth)

  test("login on invalid user fails") {
    val (users, auth) = makeTestUsersAuth
    val gen =
      for
        name     <- usernameGen
        password <- passwordGen
      yield name -> password
    forall(gen) { case (name, password) =>
      for
        x <- auth
          .login(name, password)
          .as(None)
          .recoverWith { case NoUserFound(name) =>
            Some(name).pure[IO]
          }
      yield expect.all(x.fold(false)(_ === name))
    }
  }

  test("unauthenticated user is so") {
    val (_, auth) = makeTestUsersAuth
    forall(nesGen(s => JwtToken(s))) { token =>
      for
        x <- auth.authed(token).value
      yield expect.all(x.isEmpty)
    }
  }

  test("login works") {
    val (users, auth) = makeTestUsersAuth
    forall(registerGen) { reg =>
      for
        id     <- users.create(reg)
        (t, _) <- auth.login(reg.name, reg.password)
        x      <- auth.authed(t).value
      yield expect.all(t.value === "token", x.fold(false)(_.id === id))
    }
  }

  test("logout works") {
    val (users, auth) = makeTestUsersAuth
    forall(registerGen) { reg =>
      for
        id     <- users.create(reg)
        (t, _) <- auth.login(reg.name, reg.password)
        _      <- auth.logout(id, t)
        x      <- auth.authed(t).value
      yield expect.all(x.isEmpty)
    }
  }

protected final class TestJwtTokens(tok: String = "test") extends JwtTokens[IO]:
  def create(userId: UserId): IO[JwtToken] = JwtToken(tok).pure[IO]
