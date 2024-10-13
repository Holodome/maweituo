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

trait AuthEndpointDefs(using builder: EndpointBuilderDefs):
  val `post /login` =
    builder.public
      .post
      .in("login")
      .in(jsonBody[LoginRequestDto])
      .out(jsonBody[LoginResponseDto])

  val `post /logout` =
    builder.authed
      .post
      .in("logout")
      .out(statusCode(StatusCode.NoContent))

final class AuthEndpoints[F[_]: MonadThrow](authService: AuthService[F])(using EndpointsBuilder[F])
    extends AuthEndpointDefs with Endpoints[F]:

  override val endpoints = List(
    `post /login`.serverLogic { login =>
      authService
        .login(login.toDomain)
        .map { x => LoginResponseDto(x.jwt) }
        .toOut
    },
    `post /logout`.secure.serverLogic { authed => _ =>
      given Identity = Identity(authed.id)
      authService.logout(authed.jwt).toOut
    }
  ).map(_.tag("auth"))
