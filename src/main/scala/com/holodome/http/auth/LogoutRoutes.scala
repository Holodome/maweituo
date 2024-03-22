package com.holodome.http.auth

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.users._
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.services.AuthService
import dev.profunktor.auth.jwt.JwtToken
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl

final case class LogoutRoutes[F[_]: JsonDecoder: MonadThrow](authService: AuthService[F])
    extends Http4sDsl[F] {
//  val routes = AuthedRoutes[Autherd]
}

