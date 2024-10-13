package maweituo
package http
package endpoints

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*
import maweituo.logic.search.{parseAuthorizedAdSearch, parseUnauthorizedAdSearch}

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

final class FeedEndpoints[F[_]: MonadThrow](feed: FeedService[F])(using builder: RoutesBuilder[F])
    extends Endpoints[F]:

  private val queryParams =
    query[Int]("page") and query[Option[Int]]("page_size") and query[Option[String]]("order") and
      query[Option[String]]("title") and query[Option[String]]("tags")

  private val publicFeedEndpoint =
    builder.public
      .get
      .in("feed")
      .in(queryParams)
      .out(jsonBody[FeedResponseDto])
      .serverLogic { t =>
        parseUnauthorizedAdSearch.tupled(t).flatMap { req =>
          feed.feed(req).map(FeedResponseDto.apply).toOut
        }
      }

  private val userFeedEndpoint =
    builder.authed
      .get
      .in("feed" / path[UserId]("user_id"))
      .in(queryParams)
      .out(jsonBody[FeedResponseDto])
      .serverLogic { authed => (_, page, pageSize, order, title, tags) =>
        given Identity = Identity(authed.id)
        parseAuthorizedAdSearch(page, pageSize, order, title, tags).flatMap { req =>
          feed.feed(req).map(FeedResponseDto.apply).toOut
        }
      }

  override val endpoints = List(
    publicFeedEndpoint,
    userFeedEndpoint
  ).map(_.tag("feed"))
