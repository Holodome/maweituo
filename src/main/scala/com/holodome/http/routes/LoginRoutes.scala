package com.holodome.http.routes

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.users.{InvalidPassword, LoginRequest, NoUserFound}
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.services.AuthService
import com.holodome.ext.jwt.jwt._
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl

final case class LoginRoutes[F[_]: JsonDecoder: MonadThrow](
    authService: AuthService[F]
) extends Http4sDsl[F] {
  val routes: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
    req.decodeR[LoginRequest] { login =>
      authService
        .login(login.name, login.password)
        .flatMap(Ok(_))
        .recoverWith { case NoUserFound(_) | InvalidPassword(_) =>
          Forbidden()
        }
    }
  }
}