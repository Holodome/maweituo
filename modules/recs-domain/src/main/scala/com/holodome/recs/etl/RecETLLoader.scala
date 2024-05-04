package com.holodome.recs.etl

import com.holodome.infrastructure.ObjectStorage

trait RecETLLoader[F[_]] {
  def load(locs: OBSSnapshotLocations, obs: ObjectStorage[F]): F[Unit]
}
