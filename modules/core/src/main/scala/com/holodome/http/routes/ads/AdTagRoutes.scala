package com.holodome.http.routes.ads

import com.holodome.domain.ads.AddTagRequest
import com.holodome.domain.services.AdTagService
import com.holodome.domain.users.AuthedUser
import com.holodome.http.Routes
import com.holodome.http.vars.AdIdVar

import cats.effect.Concurrent
import cats.syntax.all.*
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }

final case class AdTagRoutes[F[_]: Concurrent: JsonDecoder](tags: AdTagService[F]) extends Http4sDsl[F]:

  private val prefixPath = "/ads"

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / AdIdVar(adId) / "tag" as user =>
      ar.req.decode[AddTagRequest] { tag =>
        tags.addTag(adId, tag.tag, user.id).flatMap(Ok(_))
      }

    case ar @ DELETE -> Root / AdIdVar(adId) / "tag" as user =>
      ar.req.decode[AddTagRequest] { tag =>
        tags.removeTag(adId, tag.tag, user.id).flatMap(Ok(_))
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser]): Routes[F] =
    Routes(None, Some(Router(prefixPath -> authMiddleware(authedRoutes))))
