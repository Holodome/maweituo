package com.holodome.http.routes

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.errors.ApplicationError
import com.holodome.domain.users.RegisterRequest
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.http.HttpErrorHandler
import com.holodome.http.Routes
import com.holodome.services.UserService
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl

final case class RegisterRoutes[F[_]: MonadThrow: JsonDecoder](userService: UserService[F])
    extends Http4sDsl[F] {

  private val httpRoutes: HttpRoutes[F] =
    HttpRoutes.of { case req @ POST -> Root / "register" =>
      req.decodeR[RegisterRequest] { register =>
        userService
          .create(register)
          .flatMap(Ok(_))
      }
    }

  def routes(implicit H: HttpErrorHandler[F, ApplicationError]): Routes[F] =
    Routes[F](Some(H.handle(httpRoutes)), None)
}
