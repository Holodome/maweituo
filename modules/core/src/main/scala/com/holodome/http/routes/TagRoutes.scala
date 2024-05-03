package com.holodome.http.routes

import cats.syntax.all._
import cats.MonadThrow
import com.holodome.domain.ads.AdTag
import com.holodome.domain.errors.ApplicationError
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.http.{HttpErrorHandler, Routes}
import com.holodome.services.AdTagService
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import org.http4s.server.Router

final case class TagRoutes[F[_]: MonadThrow: JsonDecoder](tags: AdTagService[F])
    extends Http4sDsl[F] {

  private val prefixPath = "/tags"

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root =>
      tags.all.flatMap(Ok(_))

    case req @ GET -> Root / "ads" =>
      req.decodeR[AdTag] { tag =>
        tags.find(tag).flatMap(Ok(_))
      }
  }

  def routes(implicit H: HttpErrorHandler[F, ApplicationError]): Routes[F] =
    Routes(Some(Router(prefixPath -> H.handle(publicRoutes))), None)
}
