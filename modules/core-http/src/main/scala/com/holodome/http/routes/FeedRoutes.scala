package com.holodome.http.routes

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.errors.ApplicationError
import com.holodome.domain.pagination.Pagination
import com.holodome.domain.services.FeedService
import com.holodome.domain.users.AuthedUser
import com.holodome.http.vars.UserIdVar
import com.holodome.http.{HttpErrorHandler, Routes}
import com.holodome.utils.EncodeRF
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{NonNegative, Positive}
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}

final case class FeedRoutes[F[_]: MonadThrow: JsonDecoder](feed: FeedService[F])
    extends Http4sDsl[F] {

  private val prefixPath = "/feed"

  private object PageMatcher     extends OptionalQueryParamDecoderMatcher[Int]("page")
  private object PageSizeMatcher extends OptionalQueryParamDecoderMatcher[Int]("pageSize")

  private def makePagination(
      page: Option[Int],
      pageSize: Option[Int]
  ): F[Pagination] =
    (
      EncodeRF[F, Int, Int Refined Positive].encodeRF(pageSize.getOrElse(10)),
      EncodeRF[F, Int, Int Refined NonNegative].encodeRF(page.getOrElse(0))
    ).tupled.map { case (pageSize, page) =>
      Pagination(pageSize, page)
    }

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? PageMatcher(page) :? PageSizeMatcher(pageSize) =>
      makePagination(page, pageSize).flatMap(p => feed.getGlobal(p).flatMap(Ok(_)))
  }

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case GET -> Root / UserIdVar(userId) :? PageMatcher(page) :? PageSizeMatcher(
          pageSize
        ) as user =>
      if (userId == user.id) {
        makePagination(page, pageSize).flatMap(p => feed.getPersonalized(user.id, p).flatMap(Ok(_)))
      } else {
        Forbidden()
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser])(implicit
      H: HttpErrorHandler[F, ApplicationError]
  ): Routes[F] = {
    Routes(
      Some(Router(prefixPath -> H.handle(publicRoutes))),
      Some(Router(prefixPath -> H.handle(authMiddleware(authedRoutes))))
    )
  }

}
