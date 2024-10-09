package maweituo.domain

import cats.Show
import cats.derived.*

import maweituo.domain.users.UserId

final case class Pagination(pageSize: Int, page: Int) derives Show:
  inline def lower: Int = page * pageSize
  inline def upper: Int = lower + pageSize
  inline def limit      = pageSize
  inline def offset     = lower

object Pagination:
  val defaultPageSize: Int = 10

final case class PaginatedCollection[+A](
    items: List[A],
    pag: Pagination,
    totalPages: Int,
    totalItems: Int
) derives Show

object PaginatedCollection:
  def empty[A]: PaginatedCollection[A] =
    PaginatedCollection(List(), Pagination(0, 0), 0, 0)

  def apply[A](ids: List[A], pag: Pagination, totalCount: Int): PaginatedCollection[A] =
    val totalPages = (totalCount + pag.pageSize - 1) / pag.pageSize
    PaginatedCollection(
      ids,
      pag,
      totalPages,
      totalCount
    )

final case class Identity(id: UserId) derives Show

object Identity:
  given Conversion[Identity, UserId] = x => x.id
