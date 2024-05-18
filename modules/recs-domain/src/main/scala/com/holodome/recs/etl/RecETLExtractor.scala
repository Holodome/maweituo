package com.holodome.recs.etl

import com.holodome.infrastructure.ObjectStorage

trait RecETLExtractor[F[_]] {
  def extract(locs: OBSSnapshotLocations, obs: ObjectStorage[F]): F[Unit]
}
