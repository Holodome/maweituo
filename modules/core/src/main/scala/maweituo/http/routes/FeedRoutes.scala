package maweituo.http.routes

import cats.syntax.all.*
import cats.{MonadThrow, Parallel}

import maweituo.domain.Identity
import maweituo.domain.services.{AdSearchForm, FeedService, feedAuthorized, feedUnauthorized}
import maweituo.domain.users.AuthedUser
import maweituo.http.BothRoutes
import maweituo.http.dto.FeedResponseDto
import maweituo.http.vars.UserIdVar

import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, HttpRoutes, QueryParamDecoder}

final case class FeedRoutes[F[_]: MonadThrow: JsonDecoder: Parallel](feed: FeedService[F])
    extends Http4sDsl[F] with BothRoutes[F]:

  private object PageMatcher     extends OptionalQueryParamDecoderMatcher[Int]("page")
  private object PageSizeMatcher extends OptionalQueryParamDecoderMatcher[Int]("pageSize")
  private object OrderMatcher    extends OptionalQueryParamDecoderMatcher[String]("order")
  private object TitleMatcher    extends OptionalQueryParamDecoderMatcher[String]("string")
  private object TagsMatcher     extends OptionalQueryParamDecoderMatcher[String]("tags")

  override val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "feed" :? PageMatcher(page) :? PageSizeMatcher(pageSize) :? OrderMatcher(order)
        :? TitleMatcher(title) :? TagsMatcher(filterTags) =>
      val form = AdSearchForm(page, pageSize, order, filterTags, title)
      feed.feedUnauthorized(form)
        .map(FeedResponseDto.fromDomain)
        .flatMap(Ok(_))
  }

  override val authRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case GET -> Root / "feed" / UserIdVar(userId) :? PageMatcher(page) :? PageSizeMatcher(pageSize)
        :? OrderMatcher(order) :? TitleMatcher(title) :? TagsMatcher(filterTags) as user =>
      if userId == user.id then
        given Identity = Identity(user.id)
        val form       = AdSearchForm(page, pageSize, order, filterTags, title)
        feed.feedAuthorized(form)
          .map(FeedResponseDto.fromDomain)
          .flatMap(Ok(_))
      else
        Forbidden()
  }
