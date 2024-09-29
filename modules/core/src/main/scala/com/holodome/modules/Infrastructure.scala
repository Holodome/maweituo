package com.holodome.modules

import com.holodome.auth.{JwtExpire, JwtTokens}
import com.holodome.config.*
import com.holodome.domain.Id
import com.holodome.domain.users.UserId
import com.holodome.infrastructure.minio.MinioObjectStorage
import com.holodome.infrastructure.redis.RedisEphemeralDict
import com.holodome.infrastructure.{EphemeralDict, ObjectStorage}

import cats.MonadThrow
import cats.effect.Async
import cats.syntax.all.*
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import io.minio.MinioAsyncClient

sealed abstract class Infrastructure[F[_]]:
  val jwtTokens: JwtTokens[F]
  val jwtDict: EphemeralDict[F, UserId, JwtToken]
  val usersDict: EphemeralDict[F, JwtToken, UserId]
  val adImageStorage: ObjectStorage[F]

object Infrastructure:
  def make[F[_]: Async: MonadThrow](
      cfg: AppConfig,
      redis: RedisCommands[F, String, String],
      minio: MinioAsyncClient
  ): F[Infrastructure[F]] =
    (
      JwtExpire
        .make[F]
        .map(JwtTokens.make[F](_, cfg.jwt.accessSecret.value, cfg.jwt.tokenExpiration)),
      MinioObjectStorage.make[F](cfg.minio.url, minio, cfg.minio.bucket)
    ).mapN { case (tokens, images) =>
      new Infrastructure[F]:
        override val jwtTokens: JwtTokens[F] = tokens
        override val jwtDict: EphemeralDict[F, UserId, JwtToken] = RedisEphemeralDict
          .make[F](redis, cfg.jwt.tokenExpiration.value)
          .keyContramap[UserId](_.value.toString)
          .valueImap[JwtToken](JwtToken.apply, _.value)
        override val usersDict: EphemeralDict[F, JwtToken, UserId] = RedisEphemeralDict
          .make[F](redis, cfg.jwt.tokenExpiration.value)
          .keyContramap[JwtToken](_.value)
          .valueIFlatmap[UserId](Id.read[F, UserId], _.value.toString)
        override val adImageStorage: ObjectStorage[F] = images
    }
