package com.holodome.services

import cats.data.OptionT
import com.holodome.domain.advertisements._
import com.holodome.repositories.AdvertisementRepository

trait AdvertisementService[F[_]] {
  def find(id: AdvertisementId): OptionT[F, Advertisement]
  def all(): F[List[Advertisement]]
}

object AdvertisementService {
  private final class AdvertisementServiceInterpreter[F[_]](repo: AdvertisementRepository[F])
      extends AdvertisementService[F] {
    override def find(id: AdvertisementId): OptionT[F, Advertisement] = repo.find(id)

    override def all(): F[List[Advertisement]] = repo.all()
  }
}
