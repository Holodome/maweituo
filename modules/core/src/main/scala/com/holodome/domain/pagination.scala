package com.holodome.domain

import derevo.cats.show
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

object pagination {

  @derive(decoder, encoder, show)
  case class Pagination(pageSize: Int, page: Int) {
    def lower: Int =
      page * pageSize

    def upper: Int = lower + pageSize
  }

}
