package com.holodome.http

import cats.effect.Async
import com.holodome.Services
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.{HttpApp, HttpRoutes}

object HttpApi {
  def make[F[_]: Async](services: Services[F]): HttpApi[F] =
    new HttpApi[F](services) {}
}

sealed abstract class HttpApi[F[_]: Async](services: Services[F]) {
  private val userRoutes = UserHttpRoutes[F](services.users).routes

  private val routes: HttpRoutes[F] = userRoutes

  val httpApp: HttpApp[F] = routes.orNotFound
}
