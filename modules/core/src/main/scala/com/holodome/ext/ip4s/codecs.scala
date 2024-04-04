package com.holodome.ext.ip4s

import ciris.{ConfigDecoder, ConfigError, ConfigKey}
import com.comcast.ip4s.{Host, Port}

object codecs {
  implicit val portDecoder: ciris.ConfigDecoder[String, Host] =
    ciris.ConfigDecoder[String].mapOption("Host")(Host.fromString)

  implicit val hostDecoder: ciris.ConfigDecoder[String, Port] =
    ciris.ConfigDecoder[String].mapOption("Port")(Port.fromString)

}
