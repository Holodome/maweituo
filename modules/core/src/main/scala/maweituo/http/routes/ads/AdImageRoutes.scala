package maweituo
package http
package routes
package ads

import cats.effect.Concurrent

import maweituo.domain.all.*

import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.{AuthedRoutes, HttpRoutes, MediaType}

final class AdImageRoutes[F[_]: Monad: Concurrent](imageService: AdImageService[F])
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
      given Identity = Identity(user.id)
      ar.req.as[UploadImageRequestDto[F]].flatMap { contents =>
        imageService.upload(adIdVar, contents.toDomain) *> NoContent()
      }

    case DELETE -> Root / "ads" / AdIdVar(_) / "img" / ImageIdVar(img) as user =>
      given Identity = Identity(user.id)
      imageService.delete(img) *> NoContent()

  }
