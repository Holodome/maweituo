package com.holodome.http.auth

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.users._
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.ext.jwt.jwt._
import com.holodome.services.AuthService
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final case class LoginRoutes[F[_]: JsonDecoder: MonadThrow](
    authService: AuthService[F]
) extends Http4sDsl[F] {
  private val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
    req.decodeR[LoginRequest] { login =>
      authService
        .login(login.name, login.password)
        .flatMap(Ok(_))
        .recoverWith { case NoUserFound(_) | InvalidPassword(_) =>
          Forbidden()
        }
    }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
