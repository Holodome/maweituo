package com.holodome.repositories.redis

import cats.Monad
import cats.data.OptionT
import com.holodome.repositories.DictionaryRepository
import dev.profunktor.redis4cats.RedisCommands
import cats.syntax.all._

import scala.concurrent.duration.FiniteDuration

private[redis] class RedisDictionaryRepository[F[_]: Monad, A, B](
    redis: RedisCommands[F, String, String],
    expire: FiniteDuration
)(aString: A => String, bString: B => String, stringB: String => F[B])
    extends DictionaryRepository[F, A, B] {

  override def store(a: A, b: B): F[Unit] =
    redis.setEx(aString(a), bString(b), expire)

  override def delete(a: A): F[Unit] =
    redis.get(aString(a)).map(_ => ())

  override def get(a: A): OptionT[F, B] =
    OptionT(redis.get(aString(a))).flatMap(str => OptionT(stringB(str).map(Some(_))))
}
