package com.holodome.domain

import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId

object telemetry {
  case class UserTelemetry(
      user: UserId,
      clicked: Set[AdId],
      bought: Set[AdId],
      discussed: Set[AdId]
  )
}
