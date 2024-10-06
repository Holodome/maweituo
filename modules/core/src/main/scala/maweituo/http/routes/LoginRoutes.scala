package maweituo.http.routes

import cats.effect.Concurrent
import cats.syntax.all.*

import maweituo.domain.users.services.AuthService
import maweituo.http.PublicRoutes
import maweituo.http.dto.{LoginRequestDto, LoginResponseDto}

import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl

final case class LoginRoutes[F[_]: JsonDecoder: Concurrent](
    authService: AuthService[F]
) extends Http4sDsl[F] with PublicRoutes[F]:

  override def routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "login" =>
        req.decode[LoginRequestDto] { login =>
          authService
            .login(login.toDomain)
            .map { x => LoginResponseDto(x.jwt) }
            .flatMap(Ok(_))
        }
    }
