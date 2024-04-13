package com.holodome.http.routes

import cats.MonadThrow
import cats.effect.kernel.Concurrent
import cats.syntax.all._
import com.holodome.domain.ads._
import com.holodome.domain.errors.ApplicationError
import com.holodome.domain.images.ImageContentsStream
import com.holodome.domain.images.ImageContentsStream._
import com.holodome.domain.messages.SendMessageRequest
import com.holodome.domain.users.AuthedUser
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.http.vars.{AdIdVar, ChatIdVar, ImageIdVar}
import com.holodome.http.HttpErrorHandler
import com.holodome.services._
import org.http4s.{AuthedRoutes, HttpRoutes, MediaType}
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.server.{AuthMiddleware, Router}

import java.util.Base64

final case class AdvertisementRoutes[F[_]: MonadThrow: JsonDecoder: Concurrent](
    advertisementService: AdvertisementService[F],
    chatService: ChatService[F],
    msgService: MessageService[F],
    imageService: ImageService[F]
)(implicit
    H: HttpErrorHandler[F, ApplicationError]
) extends Http4sDsl[F] {
  private val prefixPath = "/ads"

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root =>
      advertisementService.all
        .flatMap(Ok(_))

    case GET -> Root / AdIdVar(adId) =>
      advertisementService
        .get(adId)
        .flatMap(Ok(_))

    case GET -> Root / AdIdVar(_) / "img" / ImageIdVar(imageId) =>
      imageService.get(imageId).flatMap { img =>
        Ok(img.data).map {
          val header =
            `Content-Type`(new MediaType(img.contentType.mainType, img.contentType.subType))
          _.putHeaders(header)
        }
      }
  }

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as user =>
      ar.req.decodeR[CreateAdRequest] { create =>
        advertisementService
          .create(user.id, create)
          .flatMap(Ok(_))
      }

    case DELETE -> Root / AdIdVar(adId) as user =>
      advertisementService.delete(adId, user.id) *> NoContent()

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

    case POST -> Root / AdIdVar(adId) / "msg" as user =>
      chatService
        .create(adId, user.id)
        .flatMap(Ok(_))

    case ar @ POST -> Root / AdIdVar(adIdVar) / "img" as user =>
      ar.req.as[ImageContentsStream[F]].flatMap { contents =>
        imageService.upload(user.id, adIdVar, contents).flatMap(Ok(_))
      }

    case DELETE -> Root / AdIdVar(_) / "img" / ImageIdVar(img) as user =>
      imageService.delete(img, user.id).flatMap(Ok(_))

    case ar @ POST -> Root / AdIdVar(adId) / "tag" as user =>
      ar.req.decodeR[AddTagRequest] { tag =>
        advertisementService.addTag(adId, tag.tag, user.id).flatMap(Ok(_))
      }

    case ar @ DELETE -> Root / AdIdVar(adId) / "tag" as user =>
      ar.req.decodeR[AddTagRequest] { tag =>
        advertisementService.removeTag(adId, tag.tag, user.id).flatMap(Ok(_))
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser]): HttpRoutes[F] =
    Router(prefixPath -> H.handle(publicRoutes <+> authMiddleware(authedRoutes)))
}
