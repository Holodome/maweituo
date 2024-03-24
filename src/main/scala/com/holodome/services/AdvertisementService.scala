package com.holodome.services

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.advertisements._
import com.holodome.domain.users.UserId
import com.holodome.repositories.AdvertisementRepository

trait AdvertisementService[F[_]] {
  def find(id: AdvertisementId): F[Advertisement]
  def all(): F[List[Advertisement]]
  def findByAuthor(authorId: UserId): F[List[Advertisement]]
  def create(authorId: UserId, create: CreateAdRequest): F[Unit]
}

object AdvertisementService {
  private final class AdvertisementServiceInterpreter[F[_]: MonadThrow](
      repo: AdvertisementRepository[F]
  ) extends AdvertisementService[F] {
    override def find(id: AdvertisementId): F[Advertisement] =
      repo.find(id).getOrElseF(InvalidAdId(id).raiseError)

    override def all(): F[List[Advertisement]] = repo.all()

    override def findByAuthor(authorId: UserId): F[List[Advertisement]] = ???

    override def create(authorId: UserId, create: CreateAdRequest): F[Unit] = ???
  }
}
