package com.holodome.recs.domain

import com.holodome.infrastructure.ObjectStorage.ObjectId

object recommendations {
  case class WeightVector(values: List[Float])

  case class OBSSnapshotLocations(
      ads: ObjectId,
      users: ObjectId,
      user_bought: ObjectId,
      user_discussed: ObjectId,
      user_created: ObjectId
  )
}
