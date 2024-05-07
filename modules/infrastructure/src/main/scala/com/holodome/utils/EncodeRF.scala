package com.holodome.utils

import cats.syntax.all._
import cats.Functor
import cats.ApplicativeThrow
import eu.timepit.refined.api.RefType
import eu.timepit.refined.api.Validate
import cats.Applicative

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
      case Right(a) => Applicative[F].pure(a)
    }

  def apply[F[_], A, T](implicit I: EncodeRF[F, A, T]): EncodeRF[F, A, T] = I

  implicit def forApplicativeThrow[F[_]: ApplicativeThrow, A, G[_, _], T, P](implicit
      ev: G[T, P] =:= A,
      rt: RefType[G],
      v: Validate[T, P]
  ): EncodeRF[F, T, A] =
    new EncodeRF[F, T, A] {
      def encodeRF(t: T): F[A] = doEncode(t)
    }
}
