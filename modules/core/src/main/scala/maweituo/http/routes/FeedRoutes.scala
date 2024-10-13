package maweituo
package http
package routes

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

final class FeedRoutes[F[_]: MonadThrow](feed: FeedService[F], builder: RoutesBuilder[F])
    extends Endpoints[F]:

  private val queryParams =
    query[Int]("page") and query[Option[Int]]("page_size") and query[Option[String]]("order") and
      query[Option[String]]("title") and query[Option[String]]("tags")

  override val endpoints = List(
    builder.public
      .get
      .in("feed")
      .in(queryParams)
      .out(jsonBody[FeedResponseDto])
      .serverLogic { t =>
        val form = AdSearchForm.apply.tupled(t)
        feed.feedUnauthorized(form)
          .map(FeedResponseDto.fromDomain)
          .toOut
      },
    builder.authed
      .get
      .in("feed" / path[UserId]("user_id"))
      .in(queryParams)
      .out(jsonBody[FeedResponseDto])
      .serverLogic { authed => (_, page, pageSize, order, title, tags) =>
        given Identity = Identity(authed.id)
        val form       = AdSearchForm(page, pageSize, order, title, tags)
        feed.feedAuthorized(form)
          .map(FeedResponseDto.fromDomain)
          .toOut
      }
  )
