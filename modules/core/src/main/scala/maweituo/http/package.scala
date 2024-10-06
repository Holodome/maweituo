package maweituo.http
import cats.effect.Async
import cats.syntax.all.*

import maweituo.domain.users.AuthedUser

import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, HttpRoutes}

sealed trait Routes[F[_]]:
  val publicRoutesOpt: Option[HttpRoutes[F]]             = None
  val authRoutesOpt: Option[AuthedRoutes[AuthedUser, F]] = None

trait PublicRoutes[F[_]] extends Routes[F]:
  override val publicRoutesOpt: Option[HttpRoutes[F]] = Some(routes)

  val routes: HttpRoutes[F]

trait UserAuthRoutes[F[_]] extends Routes[F]:
  override val authRoutesOpt: Option[AuthedRoutes[AuthedUser, F]] = Some(routes)

  val routes: AuthedRoutes[AuthedUser, F]

trait BothRoutes[F[_]] extends Routes[F]:
  override val publicRoutesOpt: Option[HttpRoutes[F]]             = Some(publicRoutes)
  override val authRoutesOpt: Option[AuthedRoutes[AuthedUser, F]] = Some(authRoutes)

  val publicRoutes: HttpRoutes[F]
  val authRoutes: AuthedRoutes[AuthedUser, F]

private def combineOptHttpRoutes[F[_]: Async](
    a: Option[HttpRoutes[F]],
    b: Option[HttpRoutes[F]]
): Option[HttpRoutes[F]] =
  (a, b) match
    case (Some(ar), Some(br)) => Some(ar <+> br)
    case (Some(_), None)      => a
    case (None, Some(_))      => b
    case (None, None)         => None

private def routesToTuple[F[_]](routes: Routes[F], auth: AuthMiddleware[F, AuthedUser]) =
  (routes.publicRoutesOpt, routes.authRoutesOpt.map(auth(_)))

def buildRoutes[F[_]: Async](routes: List[Routes[F]], auth: AuthMiddleware[F, AuthedUser]): HttpRoutes[F] =
  val publicRoutes = routes.map(_.publicRoutesOpt).reduce(combineOptHttpRoutes)
  val authRoutes   = routes.map(x => x.authRoutesOpt.map(auth)).reduce(combineOptHttpRoutes)
  combineOptHttpRoutes(publicRoutes, authRoutes).get
