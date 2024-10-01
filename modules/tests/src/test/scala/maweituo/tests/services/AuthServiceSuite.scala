package maweituo.tests.services

import cats.effect.IO
import cats.syntax.all.*

import maweituo.auth.JwtTokens
import maweituo.domain.errors.{InvalidPassword, NoUserWithName}
import maweituo.domain.services.*
import maweituo.domain.users.services.*
import maweituo.domain.users.{AuthedUser, UserId}
import maweituo.infrastructure.EphemeralDict
import maweituo.infrastructure.inmemory.InMemoryEphemeralDict
import maweituo.interp.*
import maweituo.interp.users.UserServiceInterp
import maweituo.tests.generators.*
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.InMemoryRepoFactory
import maweituo.tests.services.stubs.TelemetryServiceStub
import maweituo.utils.given

import dev.profunktor.auth.jwt.JwtToken
import org.scalacheck.Gen
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object AuthServiceSuite extends SimpleIOSuite with Checkers:

  given Logger[IO]           = NoOpLogger[IO]
  given TelemetryService[IO] = new TelemetryServiceStub[IO]

  private def jwtDict: EphemeralDict[IO, JwtToken, UserId]         = InMemoryEphemeralDict.make
  private def authedUsersDict: EphemeralDict[IO, UserId, JwtToken] = InMemoryEphemeralDict.make

  private def makeTestUsersAuth(tok: JwtToken): (UserService[IO], AuthService[IO]) =
    val tokens           = new TestJwtTokens(tok)
    val userRepo         = InMemoryRepoFactory.users
    val adRepo           = InMemoryRepoFactory.ads
    given IAMService[IO] = makeIAMService(adRepo)
    val users            = UserServiceInterp.make(userRepo)
    val auth             = AuthServiceInterp.make(userRepo, authedUsersDict, jwtDict, tokens)
    (users, auth)

  private def makeTestUsersAuth0: (UserService[IO], AuthService[IO]) =
    makeTestUsersAuth(JwtToken("test"))

  private val jwtGen: Gen[JwtToken] = nesGen(JwtToken.apply)

  test("login on invalid user fails") {
    val (users, auth) = makeTestUsersAuth0
    val gen =
      for
        name     <- usernameGen
        password <- passwordGen
      yield name -> password
    forall(gen) { case (name, password) =>
      for
        x <- auth.login(name, password).attempt
      yield expect.same(Left(NoUserWithName(name)), x)
    }
  }

  test("unauthenticated user") {
    forall(jwtGen) { token =>
      val (_, auth) = makeTestUsersAuth(token)
      for
        x <- auth.authed(token).value
      yield expect(x.isEmpty)
    }
  }

  test("login works") {
    val gen =
      for
        reg <- registerGen
        jwt <- jwtGen
      yield reg -> jwt
    forall(gen) { (reg, jwt) =>
      val (users, auth) = makeTestUsersAuth(jwt)
      for
        id     <- users.create(reg)
        (t, _) <- auth.login(reg.name, reg.password)
        x      <- auth.authed(t).value
      yield expect.same(t, jwt) and expect.same(Some(AuthedUser(id)), x)
    }
  }

  test("login with invalid password") {
    val gen =
      for
        reg  <- registerGen
        pass <- passwordGen
      yield reg -> pass
    val (users, auth) = makeTestUsersAuth0
    forall(gen) { (reg, pass) =>
      for
        _ <- users.create(reg)
        x <- auth.login(reg.name, pass).attempt
      yield expect.same(Left(InvalidPassword(reg.name)), x)
    }
  }

  test("logout works") {
    val gen =
      for
        reg <- registerGen
        jwt <- jwtGen
      yield reg -> jwt
    forall(gen) { (reg, jwt) =>
      val (users, auth) = makeTestUsersAuth(jwt)
      for
        id     <- users.create(reg)
        (t, _) <- auth.login(reg.name, reg.password)
        _      <- auth.logout(id, t)
        x      <- auth.authed(t).value
      yield expect(x.isEmpty)
    }
  }

protected final class TestJwtTokens(tok: JwtToken) extends JwtTokens[IO]:
  def create(userId: UserId): IO[JwtToken] = tok.pure[IO]
