package com.holodome.utils

import cats.syntax.all._
import cats.{ApplicativeThrow, Functor}
import eu.timepit.refined.api.{RefType, Validate}

trait EncodeRF[F[_], T, A] {
  def encodeRF(value: T): F[A]
}

object EncodeRF {
  implicit def functor[F[_]: Functor, T, A]: Functor[EncodeRF[F, T, _]] =
    new Functor[EncodeRF[F, T, _]] {
      def map[A, B](fa: EncodeRF[F, T, A])(f: A => B): EncodeRF[F, T, B] =
        new EncodeRF[F, T, B] {
          def encodeRF(value: T): F[B] = fa.encodeRF(value).map(f)
        }
    }

  private def doEncode[F[_]: ApplicativeThrow, A, G[_, _], T, P](t: T)(implicit
      ev: G[T, P] =:= A,
      rt: RefType[G],
      v: Validate[T, P]
  ): F[A] =
    RefType.applyRef(t)(ev, rt, v) match {
      case Left(e)  => RefinedEncodingFailure(e).raiseError[F, A]
      case Right(a) => a.pure[F]
    }

  def apply[F[_], T, A](implicit I: EncodeRF[F, T, A]): EncodeRF[F, T, A] = I

  implicit def forApplicativeThrow[F[_]: ApplicativeThrow, A, G[_, _], T, P](implicit
      ev: G[T, P] =:= A,
      rt: RefType[G],
      v: Validate[T, P]
  ): EncodeRF[F, T, A] =
    new EncodeRF[F, T, A] {
      def encodeRF(t: T): F[A] = doEncode(t)
    }

  def map[F[_]: Functor, T, A, B](f: A => B)(implicit E: EncodeRF[F, T, A]): EncodeRF[F, T, B] =
    EncodeRF.functor[F, T, A].map(E)(f)
}
