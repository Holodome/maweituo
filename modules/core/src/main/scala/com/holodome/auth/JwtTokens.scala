package com.holodome.auth

import cats.Monad
import cats.syntax.all._
import com.holodome.config.types._
import com.holodome.effects.GenUUID
import dev.profunktor.auth.jwt.{jwtEncode, JwtSecretKey, JwtToken}
import io.circe.syntax.EncoderOps
import pdi.jwt.{JwtAlgorithm, JwtClaim}

trait JwtTokens[F[_]] {
  def create: F[JwtToken]
}

object JwtTokens {
  def make[F[_]: GenUUID: Monad](
      jwtExpire: JwtExpire[F],
      cfg: JwtConfig
  ): JwtTokens[F] =
    new JwtTokens[F] {
      override def create: F[JwtToken] = for {
        uuid  <- GenUUID[F].make
        claim <- jwtExpire.expiresIn(JwtClaim(uuid.asJson.noSpaces), cfg.tokenExpiration)
        secretKey = JwtSecretKey(cfg.accessSecret.value.value)
        token <- jwtEncode[F](claim, secretKey, JwtAlgorithm.HS256)
      } yield token
    }
}
