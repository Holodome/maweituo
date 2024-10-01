package maweituo.http.routes.ads

import cats.effect.Concurrent
import cats.syntax.all.*

import maweituo.domain.ads.*
import maweituo.domain.ads.services.AdService
import maweituo.domain.users.{AuthedUser, UserId}
import maweituo.http.Routes
import maweituo.http.vars.AdIdVar

import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}

final case class AdRoutes[F[_]: Concurrent: JsonDecoder](adService: AdService[F]) extends Http4sDsl[F]:

  private val prefixPath = "/ads"

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / AdIdVar(adId) =>
    adService
      .get(adId)
      .flatMap(Ok(_))
  }

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as user =>
      ar.req.decode[CreateAdRequest] { create =>
        adService
          .create(user.id, create)
          .flatMap(Ok(_))
      }

    case DELETE -> Root / AdIdVar(adId) as user =>
      adService.delete(adId, user.id) *> NoContent()

    case ar @ POST -> Root / AdIdVar(adId) / "resolved" as user =>
      ar.req.decode[UserId] { id =>
        adService
          .markAsResolved(adId, user.id, id)
          .flatMap(Ok(_))
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser]): Routes[F] =
    Routes(
      Some(Router(prefixPath -> publicRoutes)),
      Some(Router(prefixPath -> authMiddleware(authedRoutes)))
    )
