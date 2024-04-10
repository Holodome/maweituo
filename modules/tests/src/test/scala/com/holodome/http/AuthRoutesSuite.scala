package com.holodome.http

import cats.effect._
import com.holodome.{HttpSuite, IOLoggedTest}
import com.holodome.auth.{JwtExpire, JwtTokens}
import com.holodome.config.types.{JwtAccessSecret, JwtTokenExpiration}
import com.holodome.domain.users.{LoginRequest, UserId}
import com.holodome.generators._
import com.holodome.http.routes.{LoginRoutes, RegisterRoutes}
import com.holodome.infrastructure.{EphemeralDict, InMemoryEphemeralDict}
import com.holodome.repositories._
import com.holodome.services.{AuthService, IAMService, UserService}
import com.holodome.services.AuthServiceSuite.mock
import dev.profunktor.auth.jwt.JwtToken
import org.http4s._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.Method._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.dsl.io._
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.DurationInt

object AuthRoutesSuite extends IOLoggedTest with HttpSuite {
  private def makeUserService = {
    val iam = IAMService.make(
      mock[AdvertisementRepository[IO]],
      mock[ChatRepository[IO]],
      mock[ImageRepository[IO]]
    )
    val usersRepo = new InMemoryUserRepository[IO]
    UserService.make(usersRepo, iam)
  }

  private def makeAuthService(users: UserService[IO])(implicit l: Logger[IO]) = {
    val jwtDict: EphemeralDict[IO, JwtToken, UserId]         = InMemoryEphemeralDict.make
    val authedUsersDict: EphemeralDict[IO, UserId, JwtToken] = InMemoryEphemeralDict.make
    JwtExpire.make[IO].map { e =>
      val tokens = JwtTokens.make[IO](e, JwtAccessSecret("test"), JwtTokenExpiration(30.seconds))
      AuthService.make[IO](users, authedUsersDict, jwtDict, tokens)
    }
  }

  ioLoggedTest("/login on invalid user fails") { implicit l =>
    forall(loginRequestGen) { login =>
      val req = POST(login, uri"/login")
      for {
        service <- makeAuthService(makeUserService)
        routes = LoginRoutes[IO](service).routes
        r <- expectHttpStatusLogged(routes, req)(Status.Forbidden)
      } yield r
    }
  }

  ioLoggedTest("/register on new user works") { implicit l =>
    forall(registerRequestGen) { register =>
      val req    = POST(register, uri"/register")
      val routes = RegisterRoutes[IO](makeUserService).routes
      expectHttpStatusLogged(routes, req)(Status.Created)
    }
  }

  ioLoggedTest("/register and login work") { implicit l =>
    forall(registerRequestGen) { register =>
      val login = LoginRequest(register.name, register.password)
      val users = makeUserService
      for {
        auth <- makeAuthService(users)
        loginRoutes    = LoginRoutes[IO](auth).routes
        registerRoutes = RegisterRoutes[IO](users).routes
        r <- expectHttpStatusLogged(
          registerRoutes,
          POST(register, uri"/register")
        )(
          Status.Created
        ) *> expectHttpStatusLogged(
          loginRoutes,
          POST(login, uri"/login")
        )(Status.Ok)
      } yield r
    }
  }
}
