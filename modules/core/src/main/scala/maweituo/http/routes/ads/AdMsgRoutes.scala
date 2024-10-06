package maweituo.http.routes.ads

import cats.MonadThrow
import cats.effect.Concurrent
import cats.syntax.all.*

import maweituo.domain.Identity
import maweituo.domain.ads.messages.*
import maweituo.domain.ads.services.MessageService
import maweituo.domain.users.AuthedUser
import maweituo.http.UserAuthRoutes
import maweituo.http.dto.{HistoryResponseDto, SendMessageRequestDto}
import maweituo.http.vars.{AdIdVar, ChatIdVar}

import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl

final case class AdMsgRoutes[F[_]: MonadThrow: JsonDecoder: Concurrent](msgService: MessageService[F])
    extends Http4sDsl[F] with UserAuthRoutes[F]:

  override val routes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case GET -> Root / AdIdVar(_) / "msg" / ChatIdVar(chatId) as user =>
      given Identity = Identity(user.id)
      msgService
        .history(chatId)
        .map(HistoryResponseDto.fromDomain)
        .flatMap(Ok(_))

    case ar @ POST -> Root / AdIdVar(_) / "msg" / ChatIdVar(chatId) as user =>
      given Identity = Identity(user.id)
      ar.req.decode[SendMessageRequestDto] { msg =>
        msgService
          .send(chatId, msg.toDomain)
          .flatMap(Ok(_))
      }
  }
