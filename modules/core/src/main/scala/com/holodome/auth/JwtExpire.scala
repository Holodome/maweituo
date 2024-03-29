package com.holodome.auth

import cats.effect.Sync
import cats.syntax.all._
import com.holodome.config.types._
import com.holodome.effects.JwtClock
import pdi.jwt.JwtClaim

trait JwtExpire[F[_]] {
  def expiresIn(claim: JwtClaim, exp: JwtTokenExpiration): F[JwtClaim]
}

object JwtExpire {
  def make[F[_]: Sync]: F[JwtExpire[F]] =
    JwtClock[F].utc map { implicit jClock => (claim: JwtClaim, exp: JwtTokenExpiration) =>
      Sync[F].delay(claim.issuedNow.expiresIn(exp.value.toMillis))
    }
}
