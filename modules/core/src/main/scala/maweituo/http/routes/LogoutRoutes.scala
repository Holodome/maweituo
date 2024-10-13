package maweituo
package http
package routes

import cats.MonadThrow

import maweituo.domain.all.*

import sttp.model.StatusCode
import sttp.tapir.*

final class LogoutRoutes[F[_]: MonadThrow](authService: AuthService[F], builder: RoutesBuilder[F])
    extends Endpoints[F]:

  override val endpoints = List(
    builder.authed
      .post
      .in("/logout")
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authed => _ =>
        given Identity = Identity(authed.id)
        authService.logout(authed.jwt).toOut
      }
  )
