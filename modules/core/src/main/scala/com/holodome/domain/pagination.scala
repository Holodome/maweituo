package com.holodome.domain

import derevo.cats.show
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

object pagination {

  @derive(decoder, encoder, show)
  case class Pagination(pageSize: Int, page: Int)

}
