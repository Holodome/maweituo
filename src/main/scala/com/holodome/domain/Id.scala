package com.holodome.domain

import cats.Functor
import cats.effect.Sync
import com.holodome.effects.GenUUID
import com.holodome.optics.IsUUID
import cats.syntax.all._

object Id {
  def make[F[_]: Functor: Sync, A: IsUUID]: F[A] =
    GenUUID[F].make.map(IsUUID[A]._UUID.get)

  def read[F[_]: Functor: Sync, A: IsUUID](str: String): F[A] =
    GenUUID[F].read(str).map(IsUUID[A]._UUID.get)
}
