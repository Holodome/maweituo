package com.holodome.http.routes

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.advertisements._
import com.holodome.domain.images.ImageContents
import com.holodome.domain.users.{AuthedUser, NoUserFound}
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.http.vars.{AdIdVar, ImageIdVar}
import com.holodome.services.{AdvertisementService, ChatService, ImageService}
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final case class AdvertisementRoutes[F[_]: MonadThrow: JsonDecoder](
    advertisementService: AdvertisementService[F],
    chatService: ChatService[F],
    imageService: ImageService[F]
) extends Http4sDsl[F] {
  private val prefixPath = "/ads"

  private object AdQueryParam extends OptionalQueryParamDecoderMatcher[AdvertisementParam]("ad")

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? AdQueryParam(ad) =>
      ad match {
        case None => Ok(advertisementService.all())
        case Some(param) =>
          param.toDomain match {
            case None => BadRequest()
            case Some(id) =>
              advertisementService
                .find(id)
                .flatMap(Ok(_))
                .recoverWith { case NoUserFound(_) =>
                  BadRequest()
                }
          }
      }

    case GET -> Root / AdIdVar(_) / "img" / ImageIdVar(imageId) =>
      imageService.get(imageId).flatMap(Ok(_))
  }

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as user =>
      ar.req.decodeR[CreateAdRequest] { create =>
        advertisementService.create(user.id, create).flatMap(Ok(_))
      }

    case DELETE -> Root / AdIdVar(adId) as user =>
      advertisementService.delete(adId, user.id) *> NoContent()

    case POST -> Root / AdIdVar(adId) / "chats" as user =>
      chatService.create(adId, user.id).flatMap(Ok(_)).recoverWith {
        case InvalidAdId(_) | CannotCreateChatWithMyself() => BadRequest()
      }

    case ar @ POST -> Root / AdIdVar(adIdVar) / "img" as user =>
      ar.req.decodeR[ImageContents] { contents =>
        imageService.upload(user.id, adIdVar, contents).flatMap(Ok(_))
      }

    case DELETE -> Root / AdIdVar(_) / "img" / ImageIdVar(img) as user =>
      imageService.delete(img, user.id).flatMap(Ok(_))
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser]): HttpRoutes[F] =
    Router(prefixPath -> (publicRoutes <+> authMiddleware(authedRoutes)))
}
