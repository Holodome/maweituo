package com.holodome.tests.repos.inmemory

import scala.collection.concurrent.TrieMap

import com.holodome.domain.ads.*
import com.holodome.domain.ads.repos.AdTagRepository

import cats.effect.Sync

private final class InMemoryAdTagRepository[F[_]: Sync] extends AdTagRepository[F]:

  private val map = new TrieMap[AdTag, Set[AdId]]

  override def getAllTags: F[List[AdTag]] =
    Sync[F] delay map.keys.toList

  override def addTagToAd(adId: AdId, tag: AdTag): F[Unit] =
    Sync[F] delay map.updateWith(tag) {
      case Some(s) => Some(s + adId)
      case None    => Some(Set(adId))
    }

  override def getAdTags(adId: AdId): F[List[AdTag]] =
    Sync[F] delay map.view.filter {
      (tag, ads) => ads.contains(adId)
    }.map(_._1).toList

  override def removeTagFromAd(adId: AdId, tag: AdTag): F[Unit] =
    Sync[F] delay map.updateWith(tag) {
      case Some(s) => Some(s - adId)
      case None    => None
    }

  override def getAllAdsByTag(tag: AdTag): F[List[AdId]] =
    Sync[F] delay map.getOrElse(tag, Set()).toList
