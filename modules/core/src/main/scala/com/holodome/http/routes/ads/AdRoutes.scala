package com.holodome.http.routes.ads

import com.holodome.domain.ads.*
import com.holodome.domain.services.AdService
import com.holodome.domain.users.{AuthedUser, UserId}
import com.holodome.http.Routes
import com.holodome.http.vars.AdIdVar

import cats.effect.Concurrent
import cats.syntax.all.*
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
