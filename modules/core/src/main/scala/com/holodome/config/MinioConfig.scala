package com.holodome.config

import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import ciris.Secret
import eu.timepit.refined.types.string.NonEmptyString

case class MinioConfig(
    host: Host,
    port: Port,
    userId: Secret[NonEmptyString],
    password: Secret[NonEmptyString],
    bucket: NonEmptyString,
    url: NonEmptyString
)
