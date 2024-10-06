package maweituo.http.routes.ads

import cats.Monad
import cats.syntax.all.*

import maweituo.domain.ads.services.ChatService
import maweituo.domain.users.AuthedUser
import maweituo.http.UserAuthRoutes
import maweituo.http.vars.{AdIdVar, ChatIdVar}

import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDsl

final case class AdChatRoutes[F[_]: Monad](chatService: ChatService[F])
    extends Http4sDsl[F] with UserAuthRoutes[F]:

  override val routes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case GET -> Root / "ads" / AdIdVar(_) / "chat" / ChatIdVar(chatId) as user =>
      chatService
        .get(chatId, user.id)
        .flatMap(Ok(_))

    case GET -> Root / "ads" / AdIdVar(adId) / "myChat" as user =>
      chatService
        .findForAdAndUser(adId, user.id)
        .fold(Ok(Json.obj(("errors", "chat not found".asJson))))(id => Ok(id))
        .flatten

    case POST -> Root / "ads" / AdIdVar(adId) / "chat" as user =>
      chatService
        .create(adId, user.id)
        .flatMap(Ok(_))
  }
