package maweituo
package http
package endpoints.users

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

final class UserEndpoints[F[_]: MonadThrow](userService: UserService[F])(using builder: RoutesBuilder[F])
    extends Endpoints[F]:

  val getUserEndpoint =
    builder.public
      .get
      .in("users" / path[UserId]("user_id"))
      .out(jsonBody[UserPublicInfoDto])
      .serverLogic { userId =>
        userService
          .get(userId)
          .map(UserPublicInfoDto.fromUser)
          .toOut
      }

  val deleteUserEndpoint =
    builder.authed
      .delete
      .in("users" / path[UserId]("user_id"))
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authed => userId =>
        given Identity = Identity(authed.id)
        userService.delete(userId).toOut
      }

  val updateUserEndpoint =
    builder.authed
      .put
      .in("users" / path[UserId]("user_id"))
      .in(jsonBody[UpdateUserRequestDto])
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authed => (userId, req) =>
        given Identity = Identity(authed.id)
        userService.update(req.toDomain(userId)).toOut
      }

  override val endpoints = List(
    getUserEndpoint,
    deleteUserEndpoint,
    updateUserEndpoint
  ).map(_.tag("users"))
