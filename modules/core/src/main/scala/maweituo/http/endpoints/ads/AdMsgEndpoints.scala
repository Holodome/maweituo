package maweituo
package http
package endpoints
package ads

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*
import maweituo.logic.search.parsePagination

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

trait AdMsgEndpointDefs(using builder: EndpointBuilderDefs):

  def `get /ads/$adId/chats/$chatId/msgs` =
    builder.authed
      .get
      .in("ads" / path[AdId]("ad_id") / "chats" / path[ChatId]("chat_id") / "msgs")
      .in(query[Int]("page") and query[Option[Int]]("page_size"))
      .out(jsonBody[HistoryResponseDto])

  def `post /ads/$adId/chats/$chatId/msgs` =
    builder.authed
      .post
      .in("ads" / path[AdId]("ad_id") / "chats" / path[ChatId]("chat_id") / "msgs")
      .in(jsonBody[SendMessageRequestDto])
      .out(statusCode(StatusCode.Created))

final class AdMsgEndpoints[F[_]: MonadThrow](msgService: MessageService[F])(using EndpointsBuilder[F])
    extends AdMsgEndpointDefs with Endpoints[F]:

  override def endpoints = List(
    `get /ads/$adId/chats/$chatId/msgs`.authedServerLogic { (_, chatId, page, pageSize) =>
      parsePagination(page, pageSize).flatMap { pag =>
        msgService
          .history(chatId, pag)
          .map(HistoryResponseDto.fromDomain(chatId, _))
      }
    },
    `post /ads/$adId/chats/$chatId/msgs`.authedServerLogic { (_, chatId, msg) =>
      msgService.send(chatId, msg.toDomain)
    }
  )
