package maweituo.interpreters.ads

import maweituo.domain.ads.repos.AdTagRepository
import maweituo.domain.ads.services.AdTagService
import maweituo.domain.ads.{AdId, AdTag}
import maweituo.domain.services.IAMService
import maweituo.domain.users.UserId

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
