package com.holodome.http

import cats.{Monad, MonadThrow}
import cats.effect.syntax.all._
import com.holodome.domain.auth._
import com.holodome.services.UserService
import org.http4s.HttpRoutes
import org.http4s.circe.{JsonDecoder, toMessageSyntax}
import org.http4s.circe.JsonDecoder
import org.http4s._
import cats.syntax.all._
import cats.MonadThrow
import cats.syntax.all._
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import io.circe._
import io.circe.syntax._
import io.circe.parser._

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
