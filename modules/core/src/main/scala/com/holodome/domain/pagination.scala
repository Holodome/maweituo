package com.holodome.domain.pagination

import cats.Show
import cats.derived.*

final case class Pagination(pageSize: Int, page: Int) derives Show:
  def lower: Int =
    page * pageSize

  def upper: Int = lower + pageSize
