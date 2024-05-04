package com.holodome.tests.repositories

import cats.effect.Sync
import com.holodome.domain.ads._
import com.holodome.domain.repositories.TagRepository

import scala.collection.concurrent.TrieMap

final class InMemoryTagRepository[F[_]: Sync] extends TagRepository[F] {

  private val map = new TrieMap[AdTag, Set[AdId]]

  override def getAllTags: F[List[AdTag]] =
    Sync[F].delay(map.keys.toList)

  override def addTag(tag: AdTag): F[Unit] =
    Sync[F].delay(map.put(tag, Set()))

  override def addTagToAd(adId: AdId, tag: AdTag): F[Unit] =
    Sync[F].delay(map.updateWith(tag) {
      case Some(s) => Some(s + adId)
      case None    => None
    })

  override def removeTagFromAd(adId: AdId, tag: AdTag): F[Unit] =
    Sync[F].delay(map.updateWith(tag) {
      case Some(s) => Some(s - adId)
      case None    => None
    })

  override def getAllAdsByTag(tag: AdTag): F[Set[AdId]] =
    Sync[F].delay(map.getOrElse(tag, Set.empty[AdId]))
}
