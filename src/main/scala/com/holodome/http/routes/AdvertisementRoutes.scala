package com.holodome.http.routes

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.advertisements._
import com.holodome.domain.users.{AuthedUser, NoUserFound}
import com.holodome.http.vars.AdIdVar
import com.holodome.services.AdvertisementService
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.server.{AuthMiddleware, Router}
import com.holodome.ext.http4s.refined.RefinedRequestDecoder

class AdvertisementRoutes[F[_]: MonadThrow](
    advertisementService: AdvertisementService[F]
) extends Http4sDsl[F] {
  private val prefixPath = "/ads"

  private object AdQueryParam extends OptionalQueryParamDecoderMatcher[AdvertisementParam]("ad")

  private val getRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root :? AdQueryParam(ad) =>
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
  }

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as user => ???
//      ar.req.decodeR[CreateAdRequest] { create =>
//        advertisementService.create(user.id, create).flatMap(Created(_))
//      }
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser]): HttpRoutes[F] =
    Router(prefixPath -> (getRoutes <+> authMiddleware(authedRoutes)))
}
