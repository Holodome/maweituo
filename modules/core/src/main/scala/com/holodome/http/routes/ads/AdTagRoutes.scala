package com.holodome.http.routes.ads

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.ads.AddTagRequest
import com.holodome.domain.errors.ApplicationError
import com.holodome.domain.services.AdService
import com.holodome.domain.users.AuthedUser
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.http.vars.AdIdVar
import com.holodome.http.{HttpErrorHandler, Routes}
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final case class AdTagRoutes[F[_]: MonadThrow: JsonDecoder](
    AdService: AdService[F]
) extends Http4sDsl[F] {

  private val prefixPath = "/ads"

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {

    case ar @ POST -> Root / AdIdVar(adId) / "tag" as user =>
      ar.req.decodeR[AddTagRequest] { tag =>
        AdService.addTag(adId, tag.tag, user.id).flatMap(Ok(_))
      }

    case ar @ DELETE -> Root / AdIdVar(adId) / "tag" as user =>
      ar.req.decodeR[AddTagRequest] { tag =>
        AdService.removeTag(adId, tag.tag, user.id).flatMap(Ok(_))
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser])(implicit
      H: HttpErrorHandler[F, ApplicationError]
  ): Routes[F] = {
    Routes(None, Some(Router(prefixPath -> H.handle(authMiddleware(authedRoutes)))))
  }
}
