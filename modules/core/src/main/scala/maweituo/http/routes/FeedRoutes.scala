package maweituo.http.routes

import maweituo.domain.pagination.Pagination
import maweituo.domain.services.FeedService
import maweituo.domain.users.AuthedUser
import maweituo.http.Routes
import maweituo.http.dto.FeedDTO
import maweituo.http.vars.UserIdVar

import cats.syntax.all.*
import cats.{MonadThrow, Parallel}
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}

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
