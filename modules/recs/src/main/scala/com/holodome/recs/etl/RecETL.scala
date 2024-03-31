package com.holodome.recs.etl

trait RecETL[F[_]] {
  def etlAll(): F[Unit]
}
