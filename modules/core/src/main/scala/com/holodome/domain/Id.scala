package com.holodome.domain

import com.holodome.effects.GenUUID
import com.holodome.utils.IsUUID

import cats.Functor
import cats.syntax.all.*

object Id:
  def make[F[_]: Functor: GenUUID, A: IsUUID]: F[A] =
    GenUUID[F].make.map(IsUUID[A].iso.get)

  def read[F[_]: Functor: GenUUID, A: IsUUID](str: String): F[A] =
    GenUUID[F].read(str).map(IsUUID[A].iso.get)
