package com.holodome.modules

import cats.effect.Async
import com.holodome.domain.users.AuthedUser
import com.holodome.http.auth.LoginRoutes
import dev.profunktor.auth.JwtAuthMiddleware
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.{HttpApp, HttpRoutes}

object HttpApi {
  def make[F[_]: Async](services: Services[F]): HttpApi[F] =
    new HttpApi[F](services) {}
}

sealed abstract class HttpApi[F[_]: Async](services: Services[F]) {
  private val authRoutes = LoginRoutes[F](services.auth).routes

  private val routes: HttpRoutes[F] = authRoutes

  val httpApp: HttpApp[F] = routes.orNotFound
}
