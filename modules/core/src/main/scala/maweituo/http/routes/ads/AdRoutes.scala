package maweituo.http.routes.ads

import cats.effect.Concurrent
import cats.syntax.all.*

import maweituo.domain.ads.*
import maweituo.domain.ads.services.AdService
import maweituo.domain.users.AuthedUser
import maweituo.http.BothRoutes
import maweituo.http.dto.{AdResponseDto, CreateAdRequestDto, CreateAdResponseDto, MarkAdResolvedRequestDto}
import maweituo.http.vars.AdIdVar

import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, HttpRoutes}

final case class AdRoutes[F[_]: Concurrent: JsonDecoder](adService: AdService[F])
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
      ar.req.decode[CreateAdRequestDto] { create =>
        adService
          .create(user.id, create.toDomain)
          .map(CreateAdResponseDto.apply)
          .flatMap(Ok(_))
      }

    case DELETE -> Root / "ads" / AdIdVar(adId) as user =>
      adService.delete(adId, user.id) *> NoContent()

    case ar @ POST -> Root / "ads" / AdIdVar(adId) / "resolved" as user =>
      ar.req.decode[MarkAdResolvedRequestDto] { req =>
        adService.markAsResolved(adId, user.id, req.withWhom) *> NoContent()
      }
  }
