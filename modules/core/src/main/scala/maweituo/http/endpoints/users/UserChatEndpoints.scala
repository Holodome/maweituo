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

final class UserChatEndpoints[F[_]: MonadThrow](userChatsService: UserChatsService[F])(using builder: RoutesBuilder[F])
    extends Endpoints[F]:

  val getUserChatsEndpoint =
    builder.authed
      .get
      .in("users" / path[UserId]("user_id") / "chats")
      .out(jsonBody[UserChatsResponseDto])
      .serverLogic { authed => userId =>
        given Identity = Identity(authed.id)
        userChatsService
          .getChats(userId)
          .map(UserChatsResponseDto.fromDomain(userId, _))
          .toOut
      }

  override val endpoints = List(
    getUserChatsEndpoint
  ).map(_.tag("users"))
