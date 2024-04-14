package com.holodome.http.routes.ads

import cats.effect.Concurrent
import cats.syntax.all._
import cats.Monad
import com.holodome.domain.errors.ApplicationError
import com.holodome.domain.images._
import com.holodome.domain.users._
import com.holodome.http.{HttpErrorHandler, Routes}
import com.holodome.http.vars.{AdIdVar, ImageIdVar}
import com.holodome.services._
import org.http4s.{AuthedRoutes, HttpRoutes, MediaType}
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.server.{AuthMiddleware, Router}

final case class AdImageRoutes[F[_]: Monad: Concurrent](
    imageService: AdImageService[F]
) extends Http4sDsl[F] {

  private val prefixPath = "/ads"

  val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
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

    case ar @ POST -> Root / AdIdVar(adIdVar) / "img" as user =>
      ar.req.as[ImageContentsStream[F]].flatMap { contents =>
        imageService.upload(user.id, adIdVar, contents).flatMap(Ok(_))
      }

    case DELETE -> Root / AdIdVar(_) / "img" / ImageIdVar(img) as user =>
      imageService.delete(img, user.id).flatMap(Ok(_))

  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser])(implicit
      H: HttpErrorHandler[F, ApplicationError]
  ): Routes[F] =
    Routes(
      Some(Router(prefixPath -> H.handle(publicRoutes))),
      Some(Router(prefixPath -> H.handle(authMiddleware(authedRoutes))))
    )
}
