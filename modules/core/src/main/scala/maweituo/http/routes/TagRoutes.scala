package maweituo.http.routes

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.ads.services.AdTagService
import maweituo.http.PublicRoutes
import maweituo.http.vars.TagVar

import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import maweituo.http.dto.AllTagsResponse
import maweituo.http.dto.TagAdsResponse

final case class TagRoutes[F[_]: MonadThrow: JsonDecoder](tags: AdTagService[F])
    extends Http4sDsl[F] with PublicRoutes[F]:

  override val routes = HttpRoutes.of[F] {
    case GET -> Root / "tags" =>
      tags.all.map(AllTagsResponse.apply).flatMap(Ok(_))

    case req @ GET -> Root / "tags" / TagVar(tag) / "ads" =>
      tags.find(tag).map(TagAdsResponse(tag, _)).flatMap(Ok(_))
  }
