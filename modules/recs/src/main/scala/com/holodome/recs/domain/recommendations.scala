package com.holodome.recs.domain

import com.holodome.infrastructure.ObjectStorage.OBSId

object recommendations {
  case class WeightVector(values: List[Float])

  case class OBSSnapshotLocations(
                                   ads: OBSId,
                                   users: OBSId,
                                   user_bought: OBSId,
                                   user_discussed: OBSId,
                                   user_created: OBSId
  )
}
