package com.holodome.http.routes.ads

import com.holodome.domain.messages.*
import com.holodome.domain.messages.SendMessageRequest.given
import com.holodome.domain.services.MessageService
import com.holodome.domain.users.AuthedUser
import com.holodome.http.Routes
import com.holodome.http.vars.{AdIdVar, ChatIdVar}

import cats.MonadThrow
import cats.effect.Concurrent
import cats.syntax.all.*
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final case class AdMsgRoutes[F[_]: MonadThrow: JsonDecoder: Concurrent](msgService: MessageService[F])
    extends Http4sDsl[F]:

  private val prefixPath = "/ads"

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case GET -> Root / AdIdVar(_) / "msg" / ChatIdVar(chatId) as user =>
      msgService
        .history(chatId, user.id)
        .flatMap(Ok(_))

    case ar @ POST -> Root / AdIdVar(_) / "msg" / ChatIdVar(chatId) as user =>
      ar.req.decode[SendMessageRequest] { msg =>
        msgService
          .send(chatId, user.id, msg)
          .flatMap(Ok(_))
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser]): Routes[F] =
    Routes(None, Some(Router(prefixPath -> authMiddleware(authedRoutes))))
