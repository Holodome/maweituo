package com.holodome.utils

import cats.Functor

trait EncodeR[T, A] {
  def encodeR(t: T): Either[Throwable, A]
  def rf: EncodeRF[Either[Throwable, _], T, A]
}

object EncodeR {
  def apply[A, T](implicit I: EncodeR[A, T]): EncodeR[A, T] = I

  implicit def instance[T, A](implicit E: EncodeRF[Either[Throwable, _], T, A]): EncodeR[T, A] =
    new EncodeR[T, A] {
      def encodeR(t: T): Either[Throwable, A]      = E.encodeRF(t)
      def rf: EncodeRF[Either[Throwable, _], T, A] = E
    }

  implicit def functor[T, A]: Functor[EncodeR[T, _]] =
    new Functor[EncodeR[T, _]] {
      def map[A, B](fa: EncodeR[T, A])(f: A => B): EncodeR[T, B] =
        new EncodeR[T, B] {
          def rf: EncodeRF[Either[Throwable, _], T, B] =
            EncodeRF.functor[Either[Throwable, _], T, A].map(fa.rf)(f)
          def encodeR(value: T): Either[Throwable, B] = rf.encodeRF(value)

        }
    }
}
