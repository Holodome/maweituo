package com.holodome.effects

import cats.effect.Sync
import cats.effect.std.Random

trait MkRandom[F[_]] {
  def make: F[Random[F]]
}

object MkRandom {
  def apply[F[_]: MkRandom]: MkRandom[F] = implicitly

  implicit def forSync[F[_]: Sync]: MkRandom[F] = new MkRandom[F] {
    override def make: F[Random[F]] = Random.javaUtilRandom[F](new java.util.Random)
  }
}
