package com.holodome.domain.pagination

import cats.Show
import cats.derived.*

final case class Pagination(pageSize: Int, page: Int) derives Show:
  inline def lower: Int = page * pageSize
  inline def upper: Int = lower + pageSize
  inline def limit      = pageSize
  inline def offset     = lower
