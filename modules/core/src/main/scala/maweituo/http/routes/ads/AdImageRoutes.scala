package maweituo.http.routes.ads

import cats.Monad
import cats.effect.Concurrent
import cats.syntax.all.*

import maweituo.domain.ads.images.*
import maweituo.domain.services.AdImageService
import maweituo.domain.users.*
import maweituo.http.BothRoutes
import maweituo.http.vars.{AdIdVar, ImageIdVar}

import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.{AuthedRoutes, HttpRoutes, MediaType}
import maweituo.http.dto.UploadImageRequestDto

final case class AdImageRoutes[F[_]: Monad: Concurrent](imageService: AdImageService[F])
    extends Http4sDsl[F] with BothRoutes[F]:

  override val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "ads" / AdIdVar(_) / "img" / ImageIdVar(imageId) =>
      imageService
        .get(imageId)
        .flatMap { img =>
          Ok(img.data)
            .map {
              val header =
                `Content-Type`(new MediaType(img.contentType.mainType, img.contentType.subType))
              _.putHeaders(header)
            }
        }
  }

  override val authRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "ads" / AdIdVar(adIdVar) / "img" as user =>
      ar.req.as[UploadImageRequestDto[F]].flatMap { contents =>
        imageService.upload(user.id, adIdVar, contents.toDomain) *> NoContent()
      }

    case DELETE -> Root / "ads" / AdIdVar(_) / "img" / ImageIdVar(img) as user =>
      imageService.delete(img, user.id) *> NoContent()

  }
