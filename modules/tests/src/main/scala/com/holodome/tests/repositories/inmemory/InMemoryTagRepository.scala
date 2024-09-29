package com.holodome.tests.repositories.inmemory

import scala.collection.concurrent.TrieMap

import com.holodome.domain.ads.*
import com.holodome.domain.repositories.TagRepository

import cats.effect.Sync

final class InMemoryTagRepository[F[_]: Sync] extends TagRepository[F]:

  private val map = new TrieMap[AdTag, Set[AdId]]

  override def getAllTags: F[List[AdTag]] =
    Sync[F] delay map.keys.toList

  override def addTagToAd(adId: AdId, tag: AdTag): F[Unit] =
    Sync[F] delay map.updateWith(tag) {
      case Some(s) => Some(s + adId)
      case None    => None
    }

  override def removeTagFromAd(adId: AdId, tag: AdTag): F[Unit] =
    Sync[F] delay map.updateWith(tag) {
      case Some(s) => Some(s - adId)
      case None    => None
    }

  override def getAllAdsByTag(tag: AdTag): F[List[AdId]] =
    Sync[F] delay map.getOrElse(tag, Set()).toList
