package com.holodome.auth

import cats.Monad
import com.holodome.config.types._
import com.holodome.effects.GenUUID
import dev.profunktor.auth.jwt.{jwtEncode, JwtSecretKey, JwtToken}
import pdi.jwt.{JwtAlgorithm, JwtClaim}
import cats.syntax.all._
import io.circe.syntax.EncoderOps

trait JwtTokens[F[_]] {
  def create: F[JwtToken]
}

object JwtTokens {
  def make[F[_]: GenUUID: Monad](
      jwtExpire: JwtExpire[F],
      secret: JwtAccessSecret,
      exp: JwtTokenExpiration
  ): JwtTokens[F] =
    new JwtTokens[F] {
      override def create: F[JwtToken] = for {
        uuid  <- GenUUID[F].make
        claim <- jwtExpire.expiresIn(JwtClaim(uuid.asJson.noSpaces), exp)
        secretKey = JwtSecretKey(secret.value)
        token <- jwtEncode[F](claim, secretKey, JwtAlgorithm.HS256)
      } yield token
    }
}
