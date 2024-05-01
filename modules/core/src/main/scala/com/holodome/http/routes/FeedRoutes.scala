package com.holodome.http.routes

import cats.syntax.all._
import cats.MonadThrow
import com.holodome.domain.pagination.Pagination
import com.holodome.domain.errors.ApplicationError
import com.holodome.domain.users.AuthedUser
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.http.{HttpErrorHandler, Routes}
import com.holodome.http.vars.UserIdVar
import com.holodome.services.FeedService
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final case class FeedRoutes[F[_]: MonadThrow: JsonDecoder](feed: FeedService[F])
    extends Http4sDsl[F] {

  private val prefixPath = "/feed"

  private object Page     extends OptionalQueryParamDecoderMatcher[Int]("page")
  private object PageSize extends OptionalQueryParamDecoderMatcher[Int]("pageSize")

  private def makePagination(page: Option[Int], pageSize: Option[Int]): Pagination =
    Pagination(pageSize.getOrElse(100), page.getOrElse(0))

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? Page(page) :? PageSize(pageSize) =>
      feed.getGlobal(makePagination(page, pageSize)).flatMap(Ok(_))
  }

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case GET -> Root / UserIdVar(userId) :? Page(page) :? PageSize(pageSize) as user =>
      if (userId == user.id) {
        feed.getPersonalized(user.id, makePagination(page, pageSize)).flatMap(Ok(_))
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
