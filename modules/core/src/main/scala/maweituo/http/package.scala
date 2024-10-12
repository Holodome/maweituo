package maweituo
package http

import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.*

import maweituo.domain.users.AuthedUser

import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.typelevel.log4cats.Logger

export maweituo.http.dto.{*, given}
export maweituo.http.vars.*
export maweituo.http.errors.*

sealed trait Routes[F[_]]:
  def publicRoutesOpt: Option[HttpRoutes[F]]             = None
  def authRoutesOpt: Option[AuthedRoutes[AuthedUser, F]] = None

trait PublicRoutes[F[_]] extends Routes[F]:
  override final def publicRoutesOpt: Option[HttpRoutes[F]] = Some(routes)

  def routes: HttpRoutes[F]

trait UserAuthRoutes[F[_]] extends Routes[F]:
  override final def authRoutesOpt: Option[AuthedRoutes[AuthedUser, F]] = Some(routes)

  def routes: AuthedRoutes[AuthedUser, F]

trait BothRoutes[F[_]] extends Routes[F]:
  override final def publicRoutesOpt: Option[HttpRoutes[F]]             = Some(publicRoutes)
  override final def authRoutesOpt: Option[AuthedRoutes[AuthedUser, F]] = Some(authRoutes)

  def publicRoutes: HttpRoutes[F]
  def authRoutes: AuthedRoutes[AuthedUser, F]

def buildRoutes[F[_]: Async: Logger](routes: List[Routes[F]], auth: AuthMiddleware[F, AuthedUser]): HttpRoutes[F] =
  val publicRoutes = routes.map(_.publicRoutesOpt).flatten.reduce(_ <+> _)
  val authRoutes   = routes.map(x => x.authRoutesOpt).flatten.reduce(_ <+> _)
  HttpDomainErrorHandler(publicRoutes <+> auth(authRoutes))
