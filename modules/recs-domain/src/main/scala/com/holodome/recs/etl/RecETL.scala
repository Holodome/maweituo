package com.holodome.recs.etl

trait RecETL[F[_]] {
  def run: F[Unit]
}
