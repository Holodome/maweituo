package com.holodome.http

import cats.MonadThrow
import com.holodome.domain.auth._
import com.holodome.services.UserService
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import cats.syntax.all._
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl

final case class UserHttpRoutes[F[_]: JsonDecoder: MonadThrow](
    userService: UserService[F]
) extends Http4sDsl[F] {

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      req.decodeR[LoginRequest] { login =>
        userService
          .login(login)
          .flatMap(Created(_))
      }
  }
}
