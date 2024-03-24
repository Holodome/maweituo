package com.holodome.http.advertisements

import cats.Monad
import com.holodome.domain.advertisements.AdvertisementParam
import com.holodome.services.AdvertisementService
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import cats.syntax.all._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.server.Router

class AdvertisementRoutes[F[_]: Monad](
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
              .fold(NotFound())(Ok(_))
              .flatten
        }
    }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
