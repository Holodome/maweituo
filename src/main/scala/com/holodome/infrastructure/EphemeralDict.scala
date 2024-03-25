package com.holodome.infrastructure

import cats.data.OptionT

trait EphemeralDictionary[F[_], A, B] {
  def store(a: A, b: B): F[Unit]
  def delete(a: A): F[Unit]
  def get(a: A): OptionT[F, B]
}
