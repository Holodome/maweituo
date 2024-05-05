package com.holodome.config

import com.comcast.ip4s.Host
import com.comcast.ip4s.Port

case class HttpServerConfig(
    host: Host,
    port: Port
)
