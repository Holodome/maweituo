package maweituo
package logic
package auth

import java.time.Clock

import scala.concurrent.duration.FiniteDuration

import cats.effect.Sync
import cats.syntax.all.*

import maweituo.infrastructure.effects.JwtClock

import pdi.jwt.JwtClaim

trait JwtExpire[F[_]]:
  def expiresIn(claim: JwtClaim, exp: FiniteDuration): F[JwtClaim]

object JwtExpire:
  def make[F[_]: Sync]: F[JwtExpire[F]] =
    JwtClock[F].utc map {
      jClock =>
        given Clock = jClock
        (claim: JwtClaim, exp: FiniteDuration) =>
          Sync[F].delay(claim.issuedNow.expiresIn(exp.toMillis))
    }
