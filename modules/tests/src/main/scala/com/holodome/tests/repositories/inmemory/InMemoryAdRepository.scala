package com.holodome.tests.repositories.inmemory

import scala.collection.concurrent.TrieMap

import com.holodome.domain.ads.{ AdId, Advertisement }
import com.holodome.domain.repositories.AdvertisementRepository
import com.holodome.domain.users.UserId

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all.*

final class InMemoryAdRepository[F[_]: Sync] extends AdvertisementRepository[F]:

  private val map = new TrieMap[AdId, Advertisement]

  override def create(ad: Advertisement): F[Unit] =
    Sync[F] delay map.addOne(ad.id -> ad)

  override def all: F[List[Advertisement]] =
    Sync[F] delay map.values.toList

  override def find(id: AdId): OptionT[F, Advertisement] =
    OptionT(Sync[F] delay map.get(id))

  override def delete(id: AdId): F[Unit] =
    Sync[F] delay map.remove(id)

  override def markAsResolved(id: AdId): F[Unit] =
    Sync[F] delay map.updateWith(id) {
      case Some(ad) => Some(ad.copy(resolved = true))
      case None     => None
    }

  override def findIdsByAuthor(userId: UserId): F[List[AdId]] =
    Sync[F] delay map.values.filter(_.authorId === userId).map(_.id).toList
