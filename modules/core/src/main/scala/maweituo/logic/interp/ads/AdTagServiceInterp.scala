package maweituo
package logic
package interp
package ads

import cats.Monad
import cats.syntax.all.*

import maweituo.domain.all.*

import org.typelevel.log4cats.syntax.*
import org.typelevel.log4cats.{Logger, LoggerFactory}

object AdTagServiceInterp:
  def make[F[_]: Monad: LoggerFactory](tags: AdTagRepo[F])(using iam: IAMService[F]): AdTagService[F] = new:
    private given Logger[F] = LoggerFactory[F].getLogger

    def all: F[List[AdTag]] =
      tags.getAllTags

    def find(tag: AdTag): F[List[AdId]] =
      tags.getAllAdsByTag(tag).map(_.toList)

    def addTag(id: AdId, tag: AdTag)(using Identity): F[Unit] =
      for
        _ <- iam.authAdModification(id)
        _ <- tags.addTagToAd(id, tag)
        _ <- info"Added tag $tag to ad $id by user ${summon[Identity]}"
      yield ()

    def removeTag(id: AdId, tag: AdTag)(using Identity): F[Unit] =
      for
        _ <- iam.authAdModification(id)
        _ <- tags.removeTagFromAd(id, tag)
        _ <- info"Removed tag $tag from ad $id by user ${summon[Identity]}"
      yield ()

    def adTags(adId: AdId): F[List[AdTag]] =
      tags.getAdTags(adId)
