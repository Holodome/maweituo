package com.holodome.recs

import com.holodome.infrastructure.ObjectStorage.OBSId

package object etl {

  case class OBSSnapshotLocations(
      ads: OBSId,
      users: OBSId,
      user_bought: OBSId,
      user_discussed: OBSId,
      user_created: OBSId
  )

}
