package com.holodome.services

import cats.{Monad, MonadThrow}
import cats.syntax.all._
import com.holodome.domain.advertisements._
import com.holodome.domain.users.UserId
import com.holodome.domain.Id
import com.holodome.effects.GenUUID
import com.holodome.repositories.AdvertisementRepository

trait AdvertisementService[F[_]] {
  def find(id: AdvertisementId): F[Advertisement]
  def all(): F[List[Advertisement]]
  def create(authorId: UserId, create: CreateAdRequest): F[Unit]
  def delete(id: AdvertisementId, userId: UserId): F[Unit]
}

object AdvertisementService {
  def make[F[_]: MonadThrow: GenUUID](repo: AdvertisementRepository[F]): AdvertisementService[F] =
    new AdvertisementServiceInterpreter(repo)

  private final class AdvertisementServiceInterpreter[F[_]: MonadThrow: GenUUID](
      repo: AdvertisementRepository[F]
  ) extends AdvertisementService[F] {
    override def find(id: AdvertisementId): F[Advertisement] =
      repo.find(id).getOrElseF(InvalidAdId(id).raiseError)

    override def all(): F[List[Advertisement]] = repo.all()

    override def create(authorId: UserId, create: CreateAdRequest): F[Unit] =
      for {
        id <- Id.make[F, AdvertisementId]
        ad = Advertisement(id, create.title, List(), List(), authorId)
        _ <- repo.create(ad)
      } yield ()

    override def delete(id: AdvertisementId, userId: UserId): F[Unit] =
      checkIfAuthor(id, userId) *> repo.delete(id)

    private def checkIfAuthor(adID: AdvertisementId, userId: UserId): F[Advertisement] =
      repo.find(adID).getOrElseF(InvalidAdId(adID).raiseError[F, Advertisement]).flatMap {
        case ad if ad.authorId === userId => Monad[F].pure(ad)
        case _                            => NotAnAuthor().raiseError[F, Advertisement]
      }
  }
}
