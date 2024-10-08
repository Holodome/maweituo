package maweituo.http.routes

import cats.syntax.all.*
import cats.{MonadThrow, Parallel}

import maweituo.domain.ads.AdSortOrder
import maweituo.domain.pagination.Pagination
import maweituo.domain.services.FeedService
import maweituo.domain.users.AuthedUser
import maweituo.http.BothRoutes
import maweituo.http.dto.FeedResponseDto
import maweituo.http.vars.UserIdVar

import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, HttpRoutes}

final case class FeedRoutes[F[_]: MonadThrow: JsonDecoder: Parallel](feed: FeedService[F])
    extends Http4sDsl[F] with BothRoutes[F]:

  private object PageMatcher     extends OptionalQueryParamDecoderMatcher[Int]("page")
  private object PageSizeMatcher extends OptionalQueryParamDecoderMatcher[Int]("pageSize")

  private def makePagination(
      page: Option[Int],
      pageSize: Option[Int]
  ): Pagination = Pagination(pageSize.getOrElse(10), page.map(_ - 1).getOrElse(0))

  override val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "feed" :? PageMatcher(page) :? PageSizeMatcher(pageSize) =>
      val p = makePagination(page, pageSize)
      feed.getGlobal(p, AdSortOrder.CreatedAtAsc)
        .map(x => FeedResponseDto(x.pag, x.adIds, x.totalPages, x.totalItems))
        .flatMap(Ok(_))
  }

  override val authRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case GET -> Root / "feed" / UserIdVar(userId) :? PageMatcher(page) :? PageSizeMatcher(pageSize) as user =>
      if userId == user.id then
        val p = makePagination(page, pageSize)
        (feed.getPersonalized(user.id, p), feed.getPersonalizedSize(user.id))
          .parMapN { case (feed, size) =>
            FeedResponseDto(p, feed, 0, size)
          }
          .flatMap(Ok(_))
      else
        Forbidden()
  }
