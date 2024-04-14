package com.holodome

import cats.syntax.all._
import cats.Semigroup
import cats.effect.Async
import org.http4s.HttpRoutes

package object http {

  case class Routes[F[_]](public: Option[HttpRoutes[F]], authed: Option[HttpRoutes[F]])

  private def combineOptHttpRoutes[F[_]: Async](
      a: Option[HttpRoutes[F]],
      b: Option[HttpRoutes[F]]
  ): Option[HttpRoutes[F]] =
    a match {
      case Some(xR) =>
        b match {
          case Some(yR) => Some(xR <+> yR)
          case None     => Some(xR)
        }
      case None => b
    }

  implicit class RoutesOps[F[_]: Async](routes: Routes[F]) {
    def collapse: HttpRoutes[F] =
      combineOptHttpRoutes(routes.public, routes.authed).get
  }

  implicit def routesSemigroup[F[_]: Async]: Semigroup[Routes[F]] =
    (x: Routes[F], y: Routes[F]) =>
      Routes(
        combineOptHttpRoutes(x.public, y.public),
        combineOptHttpRoutes(x.authed, y.authed)
      )
}
