package com.holodome.http.routes

import com.holodome.domain.services.AuthService
import com.holodome.domain.users.LoginRequest
import com.holodome.http.Routes
import com.holodome.utils.given

import cats.effect.Concurrent
import cats.syntax.all.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl

final case class LoginRoutes[F[_]: JsonDecoder: Concurrent](
    authService: AuthService[F]
) extends Http4sDsl[F]:
  private val httpRoutes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "login" =>
        req.decode[LoginRequest] { login =>
          authService
            .login(login.name, login.password)
            .map(_._1)
            .flatMap(Ok(_))
        }
    }

  def routes: Routes[F] =
    Routes[F](Some(httpRoutes), None)
