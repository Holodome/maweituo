package com.holodome.domain

import derevo.cats.show
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import eu.timepit.refined.api.Refined
import eu.timepit.refined.cats._
import eu.timepit.refined.numeric.{NonNegative, Positive}
import io.circe.refined._

package object pagination {

  @derive(decoder, encoder, show)
  case class Pagination(_pageSize: Int Refined Positive, _page: Int Refined NonNegative) {
    def pageSize: Int = _pageSize.value
    def page: Int = _page.value

    def lower: Int =
      page * pageSize

    def upper: Int = lower + pageSize
  }

}
