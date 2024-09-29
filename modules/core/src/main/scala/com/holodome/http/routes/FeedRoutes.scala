package com.holodome.http.routes

import com.holodome.domain.pagination.Pagination
import com.holodome.domain.services.FeedService
import com.holodome.domain.users.AuthedUser
import com.holodome.http.Routes
import com.holodome.http.dto.FeedDTO
import com.holodome.http.vars.UserIdVar

import cats.MonadThrow
import cats.Parallel
import cats.syntax.all.*
import org.http4s.AuthedRoutes
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.server.Router

final case class FeedRoutes[F[_]: MonadThrow: JsonDecoder: Parallel](feed: FeedService[F]) extends Http4sDsl[F]:

  private val prefixPath = "/feed"

  private object PageMatcher     extends OptionalQueryParamDecoderMatcher[Int]("page")
  private object PageSizeMatcher extends OptionalQueryParamDecoderMatcher[Int]("pageSize")

  private def makePagination(
      page: Option[Int],
      pageSize: Option[Int]
  ): Pagination = Pagination(pageSize.getOrElse(10), page.map(_ - 1).getOrElse(0))

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? PageMatcher(page) :? PageSizeMatcher(pageSize) =>
      val p = makePagination(page, pageSize)
      (feed.getGlobal(p), feed.getGlobalSize)
        .parMapN { case (feed, size) =>
          FeedDTO(feed, size)
        }
        .flatMap(Ok(_))
  }

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case GET -> Root / UserIdVar(userId) :? PageMatcher(page) :? PageSizeMatcher(pageSize) as user =>
      if userId == user.id then
        val p = makePagination(page, pageSize)
        (feed.getPersonalized(user.id, p), feed.getPersonalizedSize(user.id))
          .parMapN { case (feed, size) =>
            FeedDTO(feed, size)
          }
          .flatMap(Ok(_))
      else
        Forbidden()
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser]): Routes[F] =
    Routes(
      Some(Router(prefixPath -> publicRoutes)),
      Some(Router(prefixPath -> authMiddleware(authedRoutes)))
    )
