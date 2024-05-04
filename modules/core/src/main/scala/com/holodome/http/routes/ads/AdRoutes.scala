package com.holodome.http.routes.ads

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.ads._
import com.holodome.domain.errors.ApplicationError
import com.holodome.domain.users.AuthedUser
import com.holodome.domain.users.UserId
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.http.HttpErrorHandler
import com.holodome.http.Routes
import com.holodome.http.vars.AdIdVar
import com.holodome.services.AdService
import org.http4s.AuthedRoutes
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.server.Router

final case class AdRoutes[F[_]: MonadThrow: JsonDecoder](
    AdService: AdService[F]
) extends Http4sDsl[F] {

  private val prefixPath = "/ads"

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / AdIdVar(adId) =>
    AdService
      .get(adId)
      .flatMap(Ok(_))
  }

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as user =>
      ar.req.decodeR[CreateAdRequest] { create =>
        AdService
          .create(user.id, create)
          .flatMap(Ok(_))
      }

    case DELETE -> Root / AdIdVar(adId) as user =>
      AdService.delete(adId, user.id) *> NoContent()

    case ar @ POST -> Root / AdIdVar(adId) / "resolved" as user =>
      ar.req.decodeR[UserId] { id =>
        AdService
          .markAsResolved(adId, user.id, id)
          .flatMap(Ok(_))
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
