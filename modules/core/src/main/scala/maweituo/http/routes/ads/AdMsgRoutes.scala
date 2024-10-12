package maweituo
package http
package routes
package ads

import cats.MonadThrow
import cats.effect.Concurrent
import cats.syntax.all.*

import maweituo.domain.all.*

import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl

final class AdMsgRoutes[F[_]: MonadThrow: JsonDecoder: Concurrent](msgService: MessageService[F])
    extends Http4sDsl[F] with UserAuthRoutes[F]:

  override val routes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case GET -> Root / AdIdVar(_) / "msg" / ChatIdVar(chatId) as user =>
      given Identity = Identity(user.id)
      msgService
        .history(chatId)
        .map(HistoryResponseDto.fromDomain(chatId, _))
        .flatMap(Ok(_))

    case ar @ POST -> Root / AdIdVar(_) / "msg" / ChatIdVar(chatId) as user =>
      given Identity = Identity(user.id)
      ar.req.decode[SendMessageRequestDto] { msg =>
        msgService
          .send(chatId, msg.toDomain)
          .flatMap(Created(_))
      }
  }
