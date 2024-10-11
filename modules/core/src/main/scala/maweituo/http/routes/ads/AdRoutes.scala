package maweituo
package http
package routes
package ads

import cats.effect.Concurrent

import maweituo.domain.all.*

import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, HttpRoutes}

final class AdRoutes[F[_]: Concurrent: JsonDecoder](adService: AdService[F])
    extends Http4sDsl[F] with BothRoutes[F]:

  override val publicRoutes: HttpRoutes[F] =
    HttpRoutes.of[F] { case GET -> Root / "ads" / AdIdVar(adId) =>
      adService
        .get(adId)
        .map(AdResponseDto.fromDomain)
        .flatMap(Ok(_))
    }

  override val authRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "ads" as user =>
      given Identity = Identity(user.id)
      ar.req.decode[CreateAdRequestDto] { create =>
        adService
          .create(create.toDomain)
          .map(CreateAdResponseDto.apply)
          .flatMap(Created(_))
      }

    case DELETE -> Root / "ads" / AdIdVar(adId) as user =>
      given Identity = Identity(user.id)
      adService.delete(adId) *> NoContent()

    case ar @ PUT -> Root / "ads" / AdIdVar(adId) / "resolved" as user =>
      given Identity = Identity(user.id)
      ar.req.decode[MarkAdResolvedRequestDto] { req =>
        adService.markAsResolved(adId, req.withWhom) *> NoContent()
      }
  }
