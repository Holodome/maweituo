package maweituo.tests.repos.inmemory

import java.time.Instant

import scala.collection.concurrent.TrieMap

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all.*

import maweituo.domain.ads.repos.AdRepo
import maweituo.domain.ads.{AdId, Advertisement}
import maweituo.domain.users.UserId
import maweituo.domain.pagination.Pagination
import maweituo.domain.ads.AdSortOrder
import maweituo.domain.ads.PaginatedAdsResponse

class InMemoryAdRepo[F[_]: Sync] extends AdRepo[F]:

  private val map = new TrieMap[AdId, Advertisement]

  override def create(ad: Advertisement): F[Unit] =
    Sync[F] delay map.addOne(ad.id -> ad)

  override def all(pag: Pagination, order: AdSortOrder): F[PaginatedAdsResponse] =
    Sync[F].raiseError(new RuntimeException("InMemoryAdRepo.all unsupported"))

  override def find(id: AdId): OptionT[F, Advertisement] =
    OptionT(Sync[F] delay map.get(id))

  override def delete(id: AdId): F[Unit] =
    Sync[F] delay map.remove(id)

  override def markAsResolved(id: AdId, at: Instant): F[Unit] =
    Sync[F] delay map.updateWith(id) {
      case Some(ad) => Some(ad.copy(resolved = true, updatedAt = at))
      case None     => None
    }

  override def findIdsByAuthor(userId: UserId): F[List[AdId]] =
    Sync[F] delay map.values.filter(_.authorId === userId).map(_.id).toList
