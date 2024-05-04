package com.holodome.infrastructure

import cats.data.OptionT
import cats.syntax.all._
import cats.{Functor, Monad}

trait EphemeralDict[F[_], A, B] {
  def store(a: A, b: B): F[Unit]
  def delete(a: A): F[Unit]
  def get(a: A): OptionT[F, B]

  def keyContramap[U](f: U => A): EphemeralDict[F, U, B] = {
    val me = this
    new EphemeralDict[F, U, B] {
      override def store(a: U, b: B): F[Unit] =
        me.store(f(a), b)

      override def delete(a: U): F[Unit] =
        me.delete(f(a))

      override def get(a: U): OptionT[F, B] =
        me.get(f(a))
    }
  }

  def valueImap[V](to: B => V, from: V => B)(implicit func: Functor[F]): EphemeralDict[F, A, V] = {
    val me = this
    new EphemeralDict[F, A, V] {
      override def store(a: A, b: V): F[Unit] =
        me.store(a, from(b))

      override def delete(a: A): F[Unit] =
        me.delete(a)

      override def get(a: A): OptionT[F, V] =
        me.get(a).map(to)
    }
  }

  def valueIFlatmap[V](to: B => F[V], from: V => B)(implicit
      func: Monad[F]
  ): EphemeralDict[F, A, V] = {
    val me = this
    new EphemeralDict[F, A, V] {
      override def store(a: A, b: V): F[Unit] =
        me.store(a, from(b))

      override def delete(a: A): F[Unit] =
        me.delete(a)

      override def get(a: A): OptionT[F, V] =
        me.get(a).flatMap(b => OptionT(to(b).map(Some(_))))
    }
  }
}
