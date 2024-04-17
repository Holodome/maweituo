package com.holodome.recs.etl

import com.holodome.infrastructure.ObjectStorage
import com.holodome.infrastructure.ObjectStorage.ObjectId

trait RecETL[F[_]] {
  def run: F[Unit]
  def saveToOBS(obs: ObjectStorage[F], objectId: ObjectId): F[Unit]
}
