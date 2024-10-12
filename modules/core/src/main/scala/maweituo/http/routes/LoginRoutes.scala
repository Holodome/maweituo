package maweituo
package http
package routes

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

final class LoginRoutes[F[_]: MonadThrow](authService: AuthService[F], builder: RoutesBuilder[F])
    extends Endpoints[F]:

  override val endpoints = List(
    builder.public
      .post
      .in("login")
      .in(jsonBody[LoginRequestDto])
      .out(jsonBody[LoginResponseDto])
      .serverLogic { login =>
        authService
          .login(login.toDomain)
          .map { x => LoginResponseDto(x.jwt) }
          .toOut
      }
  )
