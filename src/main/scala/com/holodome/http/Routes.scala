package com.holodome.http

import cats.effect.{Async, Resource}
import com.holodome.Services
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.server.Server

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import org.http4s.implicits._

class Routes[F[_]: Async](services: Services[F]) {
  private val userRoutes = UserHttpRoutes[F](services.users).routes

  val routes: HttpRoutes[F] = userRoutes
}
