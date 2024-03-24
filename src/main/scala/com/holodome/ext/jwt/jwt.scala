package com.holodome.ext.jwt

import cats.Show
import dev.profunktor.auth.jwt.JwtToken
import monocle.Iso
import org.http4s.EntityEncoder

object jwt {
  implicit def jwtTokenShow: Show[JwtToken] = Show.show(_.value)
  implicit def jwtEncoder[F[_]]: EntityEncoder[F, JwtToken] =
    EntityEncoder[F, String].contramap(_.value)
}
