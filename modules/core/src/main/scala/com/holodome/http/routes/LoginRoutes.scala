package com.holodome.http.routes

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.errors.ApplicationError
import com.holodome.domain.users.LoginRequest
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.http.HttpErrorHandler
import com.holodome.http.Routes
import com.holodome.services.AuthService
import com.holodome.utils.tokenEncoder
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl

final case class LoginRoutes[F[_]: JsonDecoder: MonadThrow](
    authService: AuthService[F]
) extends Http4sDsl[F] {
  private val httpRoutes: HttpRoutes[F] =
    HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
      req.decodeR[LoginRequest] { login =>
        authService
          .login(login.name, login.password)
          .flatMap(Ok(_))
      }
    }

  def routes(implicit H: HttpErrorHandler[F, ApplicationError]): Routes[F] =
    Routes[F](Some(H.handle(httpRoutes)), None)
}
