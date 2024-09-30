package com.holodome.http.routes

import com.holodome.domain.ads.services.AdTagService
import com.holodome.http.Routes
import com.holodome.http.vars.TagVar

import cats.MonadThrow
import cats.syntax.all.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final case class TagRoutes[F[_]: MonadThrow: JsonDecoder](tags: AdTagService[F]) extends Http4sDsl[F]:
  private val prefixPath = "/tags"

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root =>
      tags.all.flatMap(Ok(_))

    case req @ GET -> Root / TagVar(tag) / "ads" =>
      tags.find(tag).flatMap(Ok(_))
  }

  def routes: Routes[F] =
    Routes(Some(Router(prefixPath -> publicRoutes)), None)
