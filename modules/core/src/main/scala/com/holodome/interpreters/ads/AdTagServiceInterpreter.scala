package com.holodome.interpreters.ads

import com.holodome.domain.ads.repos.AdTagRepository
import com.holodome.domain.ads.services.AdTagService
import com.holodome.domain.ads.{ AdId, AdTag }
import com.holodome.domain.services.IAMService
import com.holodome.domain.users.UserId

import cats.Monad
import cats.syntax.all.*
import org.typelevel.log4cats.Logger

object AdTagServiceInterpreter:
  def make[F[_]: Monad: Logger](tags: AdTagRepository[F])(using iam: IAMService[F]): AdTagService[F] = new:
    def all: F[List[AdTag]] =
      tags.getAllTags

    def find(tag: AdTag): F[List[AdId]] =
      tags.getAllAdsByTag(tag).map(_.toList)

    def addTag(id: AdId, tag: AdTag, userId: UserId): F[Unit] =
      for
        _ <- iam.authAdModification(id, userId)
        _ <- tags.addTagToAd(id, tag)
        _ <- Logger[F].info(s"Added tag $tag to ad $id by user $userId")
      yield ()

    def removeTag(id: AdId, tag: AdTag, userId: UserId): F[Unit] =
      for
        _ <- iam.authAdModification(id, userId)
        _ <- tags.removeTagFromAd(id, tag)
        _ <- Logger[F].info(s"Removed tag $tag from ad $id by user $userId")
      yield ()
