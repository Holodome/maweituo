package com.holodome.auth

import java.time.Clock

import com.holodome.config.*
import com.holodome.config.JwtTokenExpiration
import com.holodome.effects.JwtClock

import cats.effect.Sync
import cats.syntax.all.*
import pdi.jwt.JwtClaim

trait JwtExpire[F[_]]:
  def expiresIn(claim: JwtClaim, exp: JwtTokenExpiration): F[JwtClaim]

object JwtExpire:
  def make[F[_]: Sync]: F[JwtExpire[F]] =
    JwtClock[F].utc map {
      jClock =>
        given Clock = jClock
        (claim: JwtClaim, exp: JwtTokenExpiration) =>
          Sync[F].delay(claim.issuedNow.expiresIn(exp.value.toMillis))
    }
