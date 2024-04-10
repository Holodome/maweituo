package com.holodome.http.routes

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.errors.ApplicationError
import com.holodome.domain.users.LoginRequest
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.ext.jwt._
import com.holodome.http.HttpErrorHandler
import com.holodome.services.AuthService
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl

final case class LoginRoutes[F[_]: JsonDecoder: MonadThrow](
    authService: AuthService[F]
)(implicit
    H: HttpErrorHandler[F, ApplicationError]
) extends Http4sDsl[F] {
  val routes: HttpRoutes[F] = H.handle(HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
    req.decodeR[LoginRequest] { login =>
      authService
        .login(login.name, login.password)
        .flatMap(Ok(_))
    }
  })
}
