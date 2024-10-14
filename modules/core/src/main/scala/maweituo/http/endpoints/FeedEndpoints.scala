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

trait FeedEndpointDefs(using builder: EndpointBuilderDefs):

  private val queryParams =
    query[Int]("page") and query[Option[Int]]("page_size") and query[Option[String]]("order") and
      query[Option[String]]("title") and query[Option[String]]("tags")

  val `get /feed` =
    builder.public
      .get
      .in("feed")
      .in(queryParams)
      .out(jsonBody[FeedResponseDto])

  val `get /feed/$userId` =
    builder.authed
      .get
      .in("feed" / path[UserId]("user_id"))
      .in(queryParams)
      .out(jsonBody[FeedResponseDto])

final class FeedEndpoints[F[_]: MonadThrow](feed: FeedService[F])(using EndpointsBuilder[F])
    extends FeedEndpointDefs with Endpoints[F]:

  override val endpoints = List(
    `get /feed`.serverLogicF { t =>
      parseUnauthorizedAdSearch.tupled(t).flatMap { req =>
        feed.feed(req).map(FeedResponseDto.apply)
      }
    },
    `get /feed/$userId`.authedServerLogic { (_, page, pageSize, order, title, tags) =>
      parseAuthorizedAdSearch(page, pageSize, order, title, tags).flatMap { req =>
        feed.feed(req).map(FeedResponseDto.apply)
      }
    }
  ).map(_.tag("feed"))
