package com.holodome.repositories

import cats.Applicative
import cats.Monad
import cats.syntax.all._
import com.holodome.domain.ads.AdId
import com.holodome.domain.ads.AdTag

trait TagRepository[F[_]] {
  def getAllTags: F[List[AdTag]]
  def addTag(tag: AdTag): F[Unit]
  def addTagToAd(adId: AdId, tag: AdTag): F[Unit]
  def removeTagFromAd(adId: AdId, tag: AdTag): F[Unit]
  def getAllAdsByTag(tag: AdTag): F[Set[AdId]]
}

object TagRepository {
  implicit class TagRepositoryOps[F[_]: Monad](repo: TagRepository[F]) {
    def ensureCreated(tag: AdTag): F[Boolean] =
      repo.getAllTags.map(_.contains(tag)).flatTap {
        case true  => Applicative[F].unit
        case false => repo.addTag(tag)
      }
  }
}
