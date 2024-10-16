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
  def `post /login` =
    builder.public
      .post
      .in("login")
      .in(jsonBody[LoginRequestDto])
      .out(jsonBody[LoginResponseDto])

  def `post /logout` =
    builder.authed
      .post
      .in("logout")
      .out(statusCode(StatusCode.NoContent))

final class AuthEndpoints[F[_]: MonadThrow](authService: AuthService[F])(using EndpointsBuilder[F])
    extends AuthEndpointDefs with Endpoints[F]:

  override def endpoints = List(
    `post /login`.serverLogicF { login =>
      authService
        .login(login.toDomain)
        .map { x => LoginResponseDto(x.jwt) }
    },
    `post /logout`.secureServerLogic { authed => _ =>
      given Identity = Identity(authed.id)
      authService.logout(authed.jwt)
    }
  ).map(_.tag("auth"))
