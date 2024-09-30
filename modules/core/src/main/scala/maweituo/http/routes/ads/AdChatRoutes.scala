package maweituo.http.routes.ads

import maweituo.domain.ads.services.ChatService
import maweituo.domain.users.AuthedUser
import maweituo.http.Routes
import maweituo.http.vars.{AdIdVar, ChatIdVar}

import cats.Monad
import cats.syntax.all.*
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final case class AdChatRoutes[F[_]: Monad](chatService: ChatService[F]) extends Http4sDsl[F]:
  private val prefixPath = "/ads"

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case GET -> Root / AdIdVar(_) / "chat" / ChatIdVar(chatId) as user =>
      chatService
        .get(chatId, user.id)
        .flatMap(Ok(_))

    case GET -> Root / AdIdVar(adId) / "myChat" as user =>
      chatService
        .findForAdAndUser(adId, user.id)
        .fold(Ok(Json.obj(("errors", "chat not found".asJson))))(id => Ok(id))
        .flatten

    case POST -> Root / AdIdVar(adId) / "chat" as user =>
      chatService
        .create(adId, user.id)
        .flatMap(Ok(_))
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser]): Routes[F] =
    Routes(None, Some(Router(prefixPath -> authMiddleware(authedRoutes))))
