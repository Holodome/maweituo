package com.holodome.repositories

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all._
import com.holodome.domain.ads.{AdId, AdTag, Advertisement}
import com.holodome.domain.images.ImageId

import scala.collection.concurrent.TrieMap

final class InMemoryAdRepository[F[_]: Sync] extends AdvertisementRepository[F] {

  private val map = new TrieMap[AdId, Advertisement]

  override def create(ad: Advertisement): F[Unit] =
    Sync[F].delay { map.addOne(ad.id -> ad) }

  override def all(): F[List[Advertisement]] =
    Sync[F].delay { map.values.toList }

  override def find(id: AdId): OptionT[F, Advertisement] =
    OptionT(Sync[F].delay { map.get(id) })

  override def delete(id: AdId): F[Unit] =
    Sync[F].delay { map.remove(id) }

  override def addTag(id: AdId, tag: AdTag): F[Unit] =
    Sync[F].delay(map.get(id).map { ad =>
      val newAd = ad.copy(tags = ad.tags + tag)
      map.update(id, newAd)
    })

  override def addImage(id: AdId, image: ImageId): F[Unit] =
    Sync[F].delay(map.get(id).map { ad =>
      val newAd = ad.copy(images = ad.images + image)
      map.update(id, newAd)
    })

  override def removeTag(id: AdId, tag: AdTag): F[Unit] =
    Sync[F].delay(map.get(id).map { ad =>
      val newAd = ad.copy(tags = ad.tags.filter(testTag => testTag =!= tag))
      map.update(id, newAd)
    })

  override def removeImage(id: AdId, image: ImageId): F[Unit] =
    Sync[F].delay(map.get(id).map { ad =>
      val newAd = ad.copy(images = ad.images.filter(test => test =!= image))
      map.update(id, newAd)
    })
}
