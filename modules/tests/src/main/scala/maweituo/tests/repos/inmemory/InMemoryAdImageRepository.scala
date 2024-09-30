package maweituo.tests.repos.inmemory

import scala.collection.concurrent.TrieMap

import maweituo.domain.ads.AdId
import maweituo.domain.ads.images.{Image, ImageId}
import maweituo.domain.ads.repos.AdImageRepository

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all.*

private final class InMemoryAdImageRepository[F[_]: Sync] extends AdImageRepository[F]:

  private val map = new TrieMap[ImageId, Image]()

  override def create(image: Image): F[Unit] =
    Sync[F] delay map.put(image.id, image)

  override def find(imageId: ImageId): OptionT[F, Image] =
    OptionT(Sync[F] delay map.get(imageId))

  override def delete(imageId: ImageId): F[Unit] =
    Sync[F] delay map.remove(imageId)

  override def findIdsByAd(adId: AdId): F[List[ImageId]] =
    Sync[F] delay map.values.filter(_.adId === adId).map(_.id).toList
