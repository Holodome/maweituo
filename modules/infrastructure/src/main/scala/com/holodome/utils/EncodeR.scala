package com.holodome.utils

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
}
