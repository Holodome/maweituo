package maweituo
package http
package endpoints

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

final class AuthEndpoints[F[_]: MonadThrow](authService: AuthService[F])(using builder: RoutesBuilder[F])
    extends Endpoints[F]:

  val loginEndpoint =
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

  val logoutEndpoint =
    builder.authed
      .post
      .in("logout")
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authed => _ =>
        given Identity = Identity(authed.id)
        authService.logout(authed.jwt).toOut
      }

  override val endpoints = List(
    loginEndpoint,
    logoutEndpoint
  ).map(_.tag("auth"))
