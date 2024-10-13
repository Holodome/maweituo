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

trait UserChatEndpointDefs(using builder: EndpointBuilderDefs):

  val `get /users/$userId/chats` =
    builder.authed
      .get
      .in("users" / path[UserId]("user_id") / "chats")
      .out(jsonBody[UserChatsResponseDto])

final class UserChatEndpoints[F[_]: MonadThrow](userChatsService: UserChatsService[F])(using
    builder: EndpointsBuilder[F]
) extends UserChatEndpointDefs with Endpoints[F]:

  override val endpoints = List(
    `get /users/$userId/chats`.secure.serverLogic { authed => userId =>
      given Identity = Identity(authed.id)
      userChatsService
        .getChats(userId)
        .map(UserChatsResponseDto.fromDomain(userId, _))
        .toOut
    }
  ).map(_.tag("users"))
