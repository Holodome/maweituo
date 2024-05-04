package com.holodome.domain.services

import com.holodome.domain.ads.AdId
import com.holodome.domain.pagination.Pagination
import com.holodome.domain.users.UserId

trait FeedService[F[_]] {
  def getPersonalized(user: UserId, pag: Pagination): F[List[AdId]]
  def getGlobal(pag: Pagination): F[List[AdId]]
}
