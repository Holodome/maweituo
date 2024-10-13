package maweituo
package http
package endpoints.ads

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*
import maweituo.logic.search.parsePagination

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

trait AdMsgEndpointDefs(using builder: EndpointBuilderDefs):

  val `get /ads/$adId/chats/$chatId/msgs` =
    builder.authed
      .get
      .in("ads" / path[AdId]("ad_id") / "chats" / path[ChatId]("chat_id") / "msgs")
      .in(query[Int]("page") and query[Option[Int]]("page_size"))
      .out(jsonBody[HistoryResponseDto])

  val `post /ads/$adId/chats/$chatId/msgs` =
    builder.authed
      .post
      .in("ads" / path[AdId]("ad_id") / "chats" / path[ChatId]("chat_id") / "msgs")
      .in(jsonBody[SendMessageRequestDto])
      .out(statusCode(StatusCode.Created))

final class AdMsgEndpoints[F[_]: MonadThrow](msgService: MessageService[F])(using EndpointsBuilder[F])
    extends AdMsgEndpointDefs with Endpoints[F]:

  override val endpoints = List(
    `get /ads/$adId/chats/$chatId/msgs`.secure.serverLogic { authed => (_, chatId, page, pageSize) =>
      given Identity = Identity(authed.id)
      parsePagination(page, pageSize).flatMap { pag =>
        msgService
          .history(chatId, pag)
          .map(HistoryResponseDto.fromDomain(chatId, _))
          .toOut
      }
    },
    `post /ads/$adId/chats/$chatId/msgs`.secure.serverLogic { authed => (_, chatId, msg) =>
      given Identity = Identity(authed.id)
      msgService
        .send(chatId, msg.toDomain)
        .toOut
    }
  )
