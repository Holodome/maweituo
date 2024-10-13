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

class UserEndpointDefs(using builder: EndpointBuilderDefs):

  val getUserEndpoint =
    builder.public
      .get
      .in("users" / path[UserId]("user_id"))
      .out(jsonBody[UserPublicInfoDto])

  val deleteUserEndpoint =
    builder.authed
      .delete
      .in("users" / path[UserId]("user_id"))
      .out(statusCode(StatusCode.NoContent))

  val updateUserEndpoint =
    builder.authed
      .put
      .in("users" / path[UserId]("user_id"))
      .in(jsonBody[UpdateUserRequestDto])
      .out(statusCode(StatusCode.NoContent))

final class UserEndpoints[F[_]: MonadThrow](userService: UserService[F])(using EndpointsBuilder[F])
    extends UserEndpointDefs with Endpoints[F]:

  override val endpoints = List(
    getUserEndpoint.serverLogic { userId =>
      userService
        .get(userId)
        .map(UserPublicInfoDto.fromUser)
        .toOut
    },
    deleteUserEndpoint.secure.serverLogic { authed => userId =>
      given Identity = Identity(authed.id)
      userService.delete(userId).toOut
    },
    updateUserEndpoint.secure.serverLogic { authed => (userId, req) =>
      given Identity = Identity(authed.id)
      userService.update(req.toDomain(userId)).toOut
    }
  ).map(_.tag("users"))
