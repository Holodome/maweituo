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

trait UserEndpointDefs(using builder: EndpointBuilderDefs):

  val `get /users/$userId` =
    builder.public
      .get
      .in("users" / path[UserId]("user_id"))
      .out(jsonBody[UserPublicInfoDto])

  val `delete /users/$userId` =
    builder.authed
      .delete
      .in("users" / path[UserId]("user_id"))
      .out(statusCode(StatusCode.NoContent))

  val `put /users/$userId` =
    builder.authed
      .put
      .in("users" / path[UserId]("user_id"))
      .in(jsonBody[UpdateUserRequestDto])
      .out(statusCode(StatusCode.NoContent))

final class UserEndpoints[F[_]: MonadThrow](userService: UserService[F])(using EndpointsBuilder[F])
    extends UserEndpointDefs with Endpoints[F]:

  override val endpoints = List(
    `get /users/$userId`.serverLogicF { userId =>
      userService
        .get(userId)
        .map(UserPublicInfoDto.fromUser)
    },
    `delete /users/$userId`.authedServerLogic { userId =>
      userService.delete(userId)
    },
    `put /users/$userId`.authedServerLogic { (userId, req) =>
      userService.update(req.toDomain(userId))
    }
  ).map(_.tag("users"))
