package com.holodome.domain

import cats.Functor
import cats.syntax.all._
import com.holodome.effects.GenUUID
import com.holodome.optics.IsUUID

object Id {
  def make[F[_]: Functor: GenUUID, A: IsUUID]: F[A] =
    GenUUID[F].make.map(IsUUID[A]._UUID.get)

  def read[F[_]: Functor: GenUUID, A: IsUUID](str: String): F[A] =
    GenUUID[F].read(str).map(IsUUID[A]._UUID.get)
}
