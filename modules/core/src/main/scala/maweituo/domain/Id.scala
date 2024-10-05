package maweituo.domain

import cats.Functor
import cats.syntax.all.*

import maweituo.effects.GenUUID
import maweituo.utils.IsUUID

object Id:
  def make[F[_]: Functor: GenUUID, A: IsUUID]: F[A] =
    GenUUID[F].gen.map(IsUUID[A].iso.get)

  def read[F[_]: Functor: GenUUID, A: IsUUID](str: String): F[A] =
    GenUUID[F].read(str).map(IsUUID[A].iso.get)