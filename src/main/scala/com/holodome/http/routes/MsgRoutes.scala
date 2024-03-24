package com.holodome.http.routes

import cats.MonadThrow
import com.holodome.domain.users.AuthedUser
import com.holodome.http.vars.ChatIdVar
import com.holodome.services.MessageService
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.server.{AuthMiddleware, Router}
import cats.syntax.all._
import com.holodome.domain.messages._
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder

class MsgRoutes[F[_]: MonadThrow: JsonDecoder](msgService: MessageService[F]) extends Http4sDsl[F] {
  private val prefixPath = "/msg"

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case GET -> Root / ChatIdVar(chatId) as user =>
      msgService
        .history(chatId, user.id)
        .flatMap(Ok(_))
        .recoverWith { case InvalidChatId() =>
          BadRequest()
        }
    case ar @ POST -> Root / ChatIdVar(chatId) as user =>
      ar.req.decodeR[SendMessageRequest] { msg =>
        msgService
          .send(chatId, user.id, msg)
          .flatMap(Ok(_))
          .recoverWith { case InvalidChatId() =>
            BadRequest()
          }
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser]): HttpRoutes[F] =
    Router(prefixPath -> authMiddleware(authedRoutes))
}
