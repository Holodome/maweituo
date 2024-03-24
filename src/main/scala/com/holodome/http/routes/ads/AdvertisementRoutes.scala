package com.holodome.http.routes.ads

import cats.{Monad, MonadThrow}
import com.holodome.domain.advertisements.AdvertisementParam
import com.holodome.services.AdvertisementService
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import cats.syntax.all._
import com.holodome.domain.users.NoUserFound
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.server.Router

class AdvertisementRoutes[F[_]: MonadThrow](
    advertisementService: AdvertisementService[F]
) extends Http4sDsl[F] {
  private val prefixPath = "/advertisements"

  private object AdQueryParam extends OptionalQueryParamDecoderMatcher[AdvertisementParam]("ad")

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root :? AdQueryParam(ad) =>
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

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
