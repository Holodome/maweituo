package com.holodome.domain.repositories

import com.holodome.domain.ads.{ AdId, AdTag }

import cats.syntax.all.*
import cats.{ Applicative, Monad }

trait TagRepository[F[_]]:
  def getAllTags: F[List[AdTag]]
  def addTag(tag: AdTag): F[Unit]
  def addTagToAd(adId: AdId, tag: AdTag): F[Unit]
  def removeTagFromAd(adId: AdId, tag: AdTag): F[Unit]
  def getAllAdsByTag(tag: AdTag): F[Set[AdId]]

object TagRepository:
  extension [F[_]: Monad](repo: TagRepository[F])
    def ensureCreated(tag: AdTag): F[Boolean] =
      repo.getAllTags.map(_.contains(tag)).flatTap {
        case true  => Applicative[F].unit
        case false => repo.addTag(tag)
      }
