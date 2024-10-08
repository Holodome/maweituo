package maweituo.tests.http

import cats.effect.IO
import cats.syntax.all.*

import maweituo.logic.auth.JwtTokens
import maweituo.domain.services.*
import maweituo.domain.users.UserId
import maweituo.domain.users.services.{AuthService, UserService}
import maweituo.http.dto.{LoginRequestDto, LoginResponseDto, RegisterRequestDto}
import maweituo.http.routes.{LoginRoutes, RegisterRoutes}
import maweituo.infrastructure.EphemeralDict
import maweituo.infrastructure.inmemory.InMemoryEphemeralDict
import maweituo.logic.interp.AuthServiceInterp
import maweituo.logic.interp.users.UserServiceInterp
import maweituo.tests.generators.registerRequestGen
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.InMemoryRepoFactory
import maweituo.tests.services.makeIAMService
import maweituo.tests.utils.given
import maweituo.tests.{HttpSuite, WeaverLogAdapter}

import dev.profunktor.auth.jwt.JwtToken
import org.http4s.*
import org.http4s.Method.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.implicits.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.scalacheck.Checkers
import weaver.{Expectations, SimpleIOSuite}

object LoginRoutesSuite extends SimpleIOSuite with Checkers with HttpSuite:

  private val testToken = JwtToken("test")

  private def jwtDict: EphemeralDict[IO, JwtToken, UserId]         = InMemoryEphemeralDict.make
  private def authedUsersDict: EphemeralDict[IO, UserId, JwtToken] = InMemoryEphemeralDict.make

  private def makeTestUsersAuth(tokens: JwtTokens[IO]) =
    given Logger[IO]     = NoOpLogger[IO]
    val userRepo         = InMemoryRepoFactory.users
    val adRepo           = InMemoryRepoFactory.ads
    given IAMService[IO] = makeIAMService(adRepo)
    val users            = UserServiceInterp.make(userRepo)
    val auth             = AuthServiceInterp.make(userRepo, authedUsersDict, jwtDict, tokens)
    (users, auth)

  private def loginTest(name: String)(fn: (UserService[IO], AuthService[IO], Logger[IO]) => IO[Expectations]) =
    loggedTest(name) { logger =>
      val (users, auth) = makeTestUsersAuth(TestJwtTokens(testToken))
      fn(users, auth, WeaverLogAdapter(logger))
    }

  loginTest("/register and /login work as expected") { (users, auth, log) =>
    given Logger[IO] = log
    forall(registerRequestGen) { reg =>
      val regReq =
        Request[IO](
          method = Method.POST,
          uri = uri"/register"
        ).withEntity(RegisterRequestDto(reg.name, reg.email, reg.password))
      val loginReq =
        Request[IO](
          method = Method.POST,
          uri = uri"/login"
        ).withEntity(LoginRequestDto(reg.name, reg.password))
      val login    = LoginRoutes[IO](auth).routes
      val register = RegisterRoutes[IO](users).routes
      for
        x  <- expectHttpStatusLogged(register, regReq)(Status.Ok)
        x1 <- expectHttpBodyAndStatus(login, loginReq)(LoginResponseDto(testToken), Status.Ok)
      yield x and x1
    }
  }

protected final class TestJwtTokens(tok: JwtToken) extends JwtTokens[IO]:
  def create(userId: UserId): IO[JwtToken] = tok.pure[IO]
