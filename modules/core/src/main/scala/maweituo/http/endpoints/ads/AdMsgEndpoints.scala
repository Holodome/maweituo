package maweituo
package http
package endpoints.ads

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

final class AdMsgEndpoints[F[_]: MonadThrow](
    msgService: MessageService[F],
    builder: RoutesBuilder[F]
) extends Endpoints[F]:

  override val endpoints = List(
    builder.authed
      .get
      .in("ads" / path[AdId]("ad_id") / "msgs" / path[ChatId]("chat_id"))
      .out(jsonBody[HistoryResponseDto])
      .serverLogic { authed => (_, chatId) =>
        given Identity = Identity(authed.id)
        msgService
          .history(chatId)
          .map(HistoryResponseDto.fromDomain(chatId, _))
          .toOut
      },
    builder.authed
      .post
      .in("ads" / path[AdId]("ad_id") / "msgs" / path[ChatId]("chat_id"))
      .in(jsonBody[SendMessageRequestDto])
      .out(statusCode(StatusCode.Created))
      .serverLogic { authed => (_, chatId, msg) =>
        given Identity = Identity(authed.id)
        msgService
          .send(chatId, msg.toDomain)
          .toOut
      }
  )
