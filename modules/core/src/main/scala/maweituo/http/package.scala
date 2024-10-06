package maweituo.http

import cats.effect.Async
import cats.syntax.all.*

import maweituo.domain.users.AuthedUser

import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, HttpRoutes}

sealed trait Routes[F[_]]:
  def publicRoutesOpt: Option[HttpRoutes[F]]             = None
  def authRoutesOpt: Option[AuthedRoutes[AuthedUser, F]] = None

trait PublicRoutes[F[_]] extends Routes[F]:
  override def publicRoutesOpt: Option[HttpRoutes[F]] = Some(routes)

  def routes: HttpRoutes[F]

trait UserAuthRoutes[F[_]] extends Routes[F]:
  override def authRoutesOpt: Option[AuthedRoutes[AuthedUser, F]] = Some(routes)

  def routes: AuthedRoutes[AuthedUser, F]

trait BothRoutes[F[_]] extends Routes[F]:
  override def publicRoutesOpt: Option[HttpRoutes[F]]             = Some(publicRoutes)
  override def authRoutesOpt: Option[AuthedRoutes[AuthedUser, F]] = Some(authRoutes)

  def publicRoutes: HttpRoutes[F]
  def authRoutes: AuthedRoutes[AuthedUser, F]

def buildRoutes[F[_]: Async](routes: List[Routes[F]], auth: AuthMiddleware[F, AuthedUser]): HttpRoutes[F] =
  val publicRoutes = routes.map(_.publicRoutesOpt).flatten.reduce(_ <+> _)
  val authRoutes   = routes.map(x => x.authRoutesOpt.map(auth)).flatten.reduce(_ <+> _)
  publicRoutes <+> authRoutes
