package com.holodome.http

import cats.effect.Async
import com.holodome.Services
import org.http4s.HttpRoutes

class Routes[F[_]: Async](services: Services[F]) {
  private val userRoutes = UserHttpRoutes[F](services.users).routes

  val routes: HttpRoutes[F] = userRoutes
}
