package com.holodome.http

import cats.ApplicativeError
import cats.data.{Kleisli, OptionT}
import cats.implicits._
import org.http4s.{HttpRoutes, Request, Response}

object RoutesHttpErrorHandler {
  def apply[F[_], E <: Throwable](
      routes: HttpRoutes[F]
  )(handler: E => F[Response[F]])(implicit ev: ApplicativeError[F, E]): HttpRoutes[F] =
    Kleisli { req: Request[F] =>
      OptionT {
        routes.run(req).value.handleErrorWith { e => handler(e).map(Option(_)) }
      }
    }
}
