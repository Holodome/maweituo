package com.holodome.http.routes.ads

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.ads._
import com.holodome.domain.errors.ApplicationError
import com.holodome.domain.users.AuthedUser
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.http.vars.AdIdVar
import com.holodome.http.HttpErrorHandler
import com.holodome.services._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final case class AdRoutes[F[_]: MonadThrow: JsonDecoder](
    advertisementService: AdvertisementService[F]
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

  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser]): HttpRoutes[F] =
    Router(prefixPath -> H.handle(publicRoutes <+> authMiddleware(authedRoutes)))
}
