package maweituo.tests.repos.inmemory

import scala.collection.concurrent.TrieMap

import maweituo.domain.ads.repos.AdRepository
import maweituo.domain.ads.{AdId, Advertisement}
import maweituo.domain.users.UserId

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all.*

class InMemoryAdRepository[F[_]: Sync] extends AdRepository[F]:

  private val map = new TrieMap[AdId, Advertisement]

  override def create(ad: Advertisement): F[Unit] =
    Sync[F] delay map.addOne(ad.id -> ad)

  override def all: F[List[Advertisement]] =
    Sync[F] delay map.values.toList

  override def find(id: AdId): OptionT[F, Advertisement] =
    OptionT(Sync[F] delay map.get(id))

  override def delete(id: AdId): F[Unit] =
    Sync[F] delay map.remove(id)

  override def markAsResolved(id: AdId): F[Unit] =
    Sync[F] delay map.updateWith(id) {
      case Some(ad) => Some(ad.copy(resolved = true))
      case None     => None
    }

  override def findIdsByAuthor(userId: UserId): F[List[AdId]] =
    Sync[F] delay map.values.filter(_.authorId === userId).map(_.id).toList
