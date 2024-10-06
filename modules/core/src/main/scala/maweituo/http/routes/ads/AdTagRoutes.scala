package maweituo.http.routes.ads

import cats.effect.Concurrent
import cats.syntax.all.*

import maweituo.domain.ads.AddTagRequest
import maweituo.domain.ads.services.AdTagService
import maweituo.domain.users.AuthedUser
import maweituo.http.BothRoutes
import maweituo.http.vars.AdIdVar

import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, HttpRoutes}

final case class AdTagRoutes[F[_]: Concurrent: JsonDecoder](tags: AdTagService[F])
    extends Http4sDsl[F] with BothRoutes[F]:

  override val publicRoutes: HttpRoutes[F] =
    HttpRoutes.of[F] { case GET -> Root / "ads" / AdIdVar(adId) / "tag" =>
      tags.adTags(adId).flatMap(Ok(_))
    }

  override val authRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "ads" / AdIdVar(adId) / "tag" as user =>
      ar.req.decode[AddTagRequest] { tag =>
        tags.addTag(adId, tag.tag, user.id).flatMap(Ok(_))
      }

    case ar @ DELETE -> Root / "ads" / AdIdVar(adId) / "tag" as user =>
      ar.req.decode[AddTagRequest] { tag =>
        tags.removeTag(adId, tag.tag, user.id).flatMap(Ok(_))
      }
  }
