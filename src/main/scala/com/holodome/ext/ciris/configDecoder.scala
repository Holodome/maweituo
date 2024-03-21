package com.holodome.ext.ciris

import ciris.ConfigDecoder
import com.holodome.ext.derevo.Derive

object configDecoder extends Derive[Decoder.Id]

object Decoder {
  type Id[A] = ConfigDecoder[String, A]
}
