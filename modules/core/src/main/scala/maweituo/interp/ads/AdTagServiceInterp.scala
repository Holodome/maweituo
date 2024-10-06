package maweituo.interp.ads

import cats.Monad
import cats.syntax.all.*

import maweituo.domain.Identity
import maweituo.domain.ads.repos.AdTagRepo
import maweituo.domain.ads.services.AdTagService
import maweituo.domain.ads.{AdId, AdTag}
import maweituo.domain.services.IAMService

import org.typelevel.log4cats.Logger

object AdTagServiceInterp:
  def make[F[_]: Monad: Logger](tags: AdTagRepo[F])(using iam: IAMService[F]): AdTagService[F] = new:
    def all: F[List[AdTag]] =
      tags.getAllTags

    def find(tag: AdTag): F[List[AdId]] =
      tags.getAllAdsByTag(tag).map(_.toList)

    def addTag(id: AdId, tag: AdTag)(using Identity): F[Unit] =
      for
        _ <- iam.authAdModification(id)
        _ <- tags.addTagToAd(id, tag)
        _ <- Logger[F].info(s"Added tag $tag to ad $id by user ${summon[Identity]}")
      yield ()

    def removeTag(id: AdId, tag: AdTag)(using Identity): F[Unit] =
      for
        _ <- iam.authAdModification(id)
        _ <- tags.removeTagFromAd(id, tag)
        _ <- Logger[F].info(s"Removed tag $tag from ad $id by user ${summon[Identity]}")
      yield ()

    def adTags(adId: AdId): F[List[AdTag]] =
      tags.getAdTags(adId)
