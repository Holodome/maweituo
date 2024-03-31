package com.holodome.recs.domain

import io.estatico.newtype.macros.newtype

object recommendations {
  case class WeightVector(values: List[Float])
}
