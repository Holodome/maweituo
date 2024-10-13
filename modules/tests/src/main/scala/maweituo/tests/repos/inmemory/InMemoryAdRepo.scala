package maweituo
package tests
package repos
package inmemory

import scala.collection.concurrent.TrieMap

import maweituo.domain.ads.Advertisement

class InMemoryAdRepo[F[_]: Sync] extends AdRepo[F]:

  private val map = new TrieMap[AdId, Advertisement]

  override def create(ad: Advertisement): F[Unit] =
    Sync[F] delay map.addOne(ad.id -> ad)

  override def find(id: AdId): OptionT[F, Advertisement] =
    OptionT(Sync[F] delay map.get(id))

  override def delete(id: AdId): F[Unit] =
    Sync[F] delay map.remove(id)

  override def update(update: UpdateAdRepoRequest): F[Unit] =
    Sync[F] delay map.get(update.id).map { value =>
      val newAd = Advertisement(
        update.id,
        value.authorId,
        update.title.getOrElse(value.title),
        update.resolved.getOrElse(value.resolved),
        value.createdAt,
        update.at
      )
      map.addOne(value.id -> newAd)
    }

  override def findIdsByAuthor(userId: UserId): F[List[AdId]] =
    Sync[F] delay map.values.filter(_.authorId === userId).map(_.id).toList
