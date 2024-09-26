package com.holodome.domain.repositories

import com.holodome.domain.ads.*
import com.holodome.domain.errors.InvalidAdId

import cats.MonadThrow
import cats.data.OptionT
import com.holodome.domain.users.UserId

trait AdvertisementRepository[F[_]]:
  def create(ad: Advertisement): F[Unit]
  def all: F[List[Advertisement]]
  def find(id: AdId): OptionT[F, Advertisement]
  def findIdsByAuthor(userId: UserId): F[List[AdId]]
  def markAsResolved(id: AdId): F[Unit]
  def delete(id: AdId): F[Unit]

object AdvertisementRepository:
  extension [F[_]: MonadThrow](repo: AdvertisementRepository[F])
    def get(id: AdId): F[Advertisement] =
      repo.find(id).getOrRaise(InvalidAdId(id))
