package com.holodome.repositories.redis

import cats.{Functor, Show}
import cats.data.OptionT
import com.holodome.repositories.DictionaryRepository
import dev.profunktor.redis4cats.RedisCommands
import cats.syntax.all._
import com.holodome.optics.IsNaiveString

import scala.concurrent.duration.FiniteDuration

private[redis] class RedisDictionaryRepository[F[_]: Functor, A: Show, B: Show: IsNaiveString](
    redis: RedisCommands[F, String, String],
    expire: FiniteDuration
) extends DictionaryRepository[F, A, B] {

  override def store(a: A, b: B): F[Unit] =
    redis.setEx(Show[A].show(a), Show[B].show(b), expire)

  override def delete(a: A): F[Unit] =
    redis.get(Show[A].show(a)).map(_ => ())

  override def get(a: A): OptionT[F, B] =
    OptionT(redis.get(Show[A].show(a))).map(IsNaiveString[B]._String.get(_))
}
