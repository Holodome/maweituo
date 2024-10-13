package maweituo
package http
package endpoints.ads

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*
import maweituo.logic.DomainError
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import maweituo.logic.search.parsePagination

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
      .in("ads" / path[AdId]("ad_id") / "chats" / path[ChatId]("chat_id") / "msgs")
      .in(query[Int]("page") and query[Option[Int]]("page_size"))
      .out(jsonBody[HistoryResponseDto])
      .serverLogic { authed => (_, chatId, page, pageSize) =>
        given Identity = Identity(authed.id)
        parsePagination(page, pageSize).flatMap { pag =>
          msgService
            .history(chatId, pag)
            .map(HistoryResponseDto.fromDomain(chatId, _))
            .toOut
        }
      },
    builder.authed
      .post
      .in("ads" / path[AdId]("ad_id") / "chats" / path[ChatId]("chat_id") / "msgs")
      .in(jsonBody[SendMessageRequestDto])
      .out(statusCode(StatusCode.Created))
      .serverLogic { authed => (_, chatId, msg) =>
        given Identity = Identity(authed.id)
        msgService
          .send(chatId, msg.toDomain)
          .toOut
      }
  )
