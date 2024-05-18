package com.holodome.auth

import cats.Monad
import cats.syntax.all._
import com.holodome.config.types._
import com.holodome.domain.users.UserId
import dev.profunktor.auth.jwt.{JwtSecretKey, JwtToken, jwtEncode}
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import pdi.jwt.{JwtAlgorithm, JwtClaim}

trait JwtTokens[F[_]] {
  def create(userId: UserId): F[JwtToken]
}

object JwtTokens {
  def make[F[_]: Monad](
      jwtExpire: JwtExpire[F],
      secret: JwtAccessSecret,
      exp: JwtTokenExpiration
  ): JwtTokens[F] = {
    implicit val encoder: Encoder[UserId] = Encoder.forProduct1("user_id")(_.value)
    (userId: UserId) =>
      for {
        claim <- jwtExpire.expiresIn(
          JwtClaim(userId.asJson(encoder).noSpaces),
          exp
        )
        secretKey = JwtSecretKey(secret.value)
        token <- jwtEncode[F](claim, secretKey, JwtAlgorithm.HS256)
      } yield token
  }
}
