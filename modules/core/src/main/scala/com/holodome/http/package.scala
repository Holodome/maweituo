package com.holodome

import cats.Semigroup
import cats.effect.Async
import cats.syntax.all.*
import org.http4s.HttpRoutes

package object http:

  final case class Routes[F[_]](public: Option[HttpRoutes[F]], authed: Option[HttpRoutes[F]])

  private def combineOptHttpRoutes[F[_]: Async](
      a: Option[HttpRoutes[F]],
      b: Option[HttpRoutes[F]]
  ): Option[HttpRoutes[F]] =
    (a, b) match
      case (Some(ar), Some(br)) => Some(ar <+> br)
      case (Some(_), None)      => a
      case (None, Some(_))      => b
      case (None, None)         => None

  extension [F[_]: Async](routes: Routes[F])
    def collapse: HttpRoutes[F] =
      combineOptHttpRoutes(routes.public, routes.authed).get

  given [F[_]: Async]: Semigroup[Routes[F]] =
    (x: Routes[F], y: Routes[F]) =>
      Routes(
        combineOptHttpRoutes(x.public, y.public),
        combineOptHttpRoutes(x.authed, y.authed)
      )
