package maweituo.domain.ads.repos

import cats.data.NonEmptyList

import maweituo.domain.ads.*
import maweituo.domain.{PaginatedCollection, Pagination}

trait AdSearchRepo[F[_]]:
  def all(pag: Pagination, order: AdSortOrder): F[PaginatedCollection[AdId]]
  def allFiltered(pag: Pagination, order: AdSortOrder, allowedTags: NonEmptyList[AdTag]): F[PaginatedCollection[AdId]]
