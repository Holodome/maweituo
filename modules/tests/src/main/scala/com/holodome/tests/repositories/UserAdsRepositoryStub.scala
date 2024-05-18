package com.holodome.tests.repositories

import cats.data.OptionT
import cats.effect.IO
import com.holodome.domain.ads.AdId
import com.holodome.domain.repositories.UserAdsRepository
import com.holodome.domain.users.UserId

final class UserAdsRepositoryStub extends UserAdsRepository[IO] {

  override def create(userId: UserId, ad: AdId): IO[Unit] = IO.unit

  override def get(userId: UserId): OptionT[IO, Set[AdId]] = OptionT(IO.pure(Option.empty))

}
