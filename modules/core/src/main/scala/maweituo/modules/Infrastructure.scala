package maweituo
package modules

import cats.effect.Async
import cats.syntax.all.*
import cats.{Functor, Monad, MonadThrow}

import maweituo.config.*
import maweituo.domain.users.UserId
import maweituo.infrastructure.effects.GenUUID
import maweituo.infrastructure.minio.{MinioConnection, MinioObjectStorage}
import maweituo.infrastructure.redis.RedisEphemeralDict
import maweituo.infrastructure.{EphemeralDict, ObjectStorage}
import maweituo.logic.auth.{JwtExpire, JwtTokens}
import maweituo.utils.Id

import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import org.typelevel.log4cats.Logger

sealed abstract class Infrastructure[F[_]]:
  val jwtTokens: JwtTokens[F]
  val jwtDict: EphemeralDict[F, UserId, JwtToken]
  val usersDict: EphemeralDict[F, JwtToken, UserId]
  val adImageStorage: ObjectStorage[F]

object Infrastructure:
  def make[F[_]: Async: MonadThrow: Logger](
      cfg: AppConfig,
      redis: RedisCommands[F, String, String],
      minio: MinioConnection
  ): F[Infrastructure[F]] =
    (
      JwtExpire
        .make[F]
        .map(JwtTokens.make[F](_, cfg.jwt.accessSecret.value, cfg.jwt.tokenExpiration)),
      MinioObjectStorage.make[F](minio, cfg.minio.bucket)
    ).mapN { case (tokens, images) =>
      new Infrastructure[F]:
        override val jwtTokens: JwtTokens[F] = tokens
        override val jwtDict: EphemeralDict[F, UserId, JwtToken] =
          ephemeralDictToJwt(RedisEphemeralDict.make[F](redis, cfg.jwt.tokenExpiration))
        override val usersDict: EphemeralDict[F, JwtToken, UserId] =
          ephemeralDictToUsers(RedisEphemeralDict.make[F](redis, cfg.jwt.tokenExpiration))
        override val adImageStorage: ObjectStorage[F] = images
    }

  def ephemeralDictToJwt[F[_]: Functor](dict: EphemeralDict[F, String, String]): EphemeralDict[F, UserId, JwtToken] =
    dict
      .keyContramap[UserId](_.value.toString)
      .valueImap[JwtToken](JwtToken.apply, _.value)

  def ephemeralDictToUsers[F[_]: Monad: GenUUID](dict: EphemeralDict[F, String, String])
      : EphemeralDict[F, JwtToken, UserId] =
    dict
      .keyContramap[JwtToken](_.value)
      .valueIFlatmap[UserId](Id.read[F, UserId], _.value.toString)
