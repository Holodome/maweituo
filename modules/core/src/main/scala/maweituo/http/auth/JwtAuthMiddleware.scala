package maweituo
package http
package auth

import cats.data.Kleisli

import dev.profunktor.auth.AuthHeaders
import dev.profunktor.auth.jwt.{JwtAuth, JwtToken, jwtDecode}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`WWW-Authenticate`
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, Challenge, Request}
import pdi.jwt.*
import pdi.jwt.exceptions.JwtException

object JwtAuthMiddleware:
  def apply[F[_]: MonadThrow, A](
      jwtAuth: JwtAuth,
      authenticate: JwtToken => JwtClaim => F[Option[A]]
  ): AuthMiddleware[F, A] =
    apply(jwtAuth.pure, authenticate)

  def apply[F[_]: MonadThrow, A](
      jwtAuth: F[JwtAuth],
      authenticate: JwtToken => JwtClaim => F[Option[A]]
  ): AuthMiddleware[F, A] =
    val dsl = new Http4sDsl[F] {}; import dsl.*

    val onFailure: AuthedRoutes[String, F] =
      Kleisli(req => OptionT.liftF(Unauthorized(`WWW-Authenticate`(Challenge("Bearer", "")))))

    val authUser: Kleisli[F, Request[F], Either[String, A]] =
      Kleisli { request =>
        AuthHeaders.getBearerToken(request).fold("Bearer token not found".asLeft[A].pure[F]) { token =>
          jwtAuth.flatMap(auth =>
            jwtDecode[F](token, auth)
              .flatMap(authenticate(token))
              .map(_.fold("not found".asLeft[A])(_.asRight[String]))
              .recover {
                case _: JwtException => "Invalid access token".asLeft[A]
              }
          )
        }
      }
    AuthMiddleware(authUser, onFailure)