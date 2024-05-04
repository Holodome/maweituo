package com.holodome.http.routes.ads

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.errors.ApplicationError
import com.holodome.domain.messages.SendMessageRequest
import com.holodome.domain.services.MessageService
import com.holodome.domain.users.AuthedUser
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.http.vars.{AdIdVar, ChatIdVar}
import com.holodome.http.{HttpErrorHandler, Routes}
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final case class AdMsgRoutes[F[_]: MonadThrow: JsonDecoder](
    msgService: MessageService[F]
) extends Http4sDsl[F] {

  private val prefixPath = "/ads"

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {

    case GET -> Root / AdIdVar(_) / "msg" / ChatIdVar(chatId) as user =>
      msgService
        .history(chatId, user.id)
        .flatMap(Ok(_))

    case ar @ POST -> Root / AdIdVar(_) / "msg" / ChatIdVar(chatId) as user =>
      ar.req.decodeR[SendMessageRequest] { msg =>
        msgService
          .send(chatId, user.id, msg)
          .flatMap(Ok(_))
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser])(implicit
      H: HttpErrorHandler[F, ApplicationError]
  ): Routes[F] =
    Routes(None, Some(Router(prefixPath -> H.handle(authMiddleware(authedRoutes)))))
}
