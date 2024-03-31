package com.holodome.recs.services

import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId
import com.holodome.recs.repositories.TelemetryRepository
import com.holodome.services.TelemetryService

private final class TelemetryServiceInterpreter[F[_]](repo: TelemetryRepository[F])
  extends TelemetryService[F] {

  override def userClicked(user: UserId, ad: AdId): F[Unit] =
    repo.userClicked(user, ad)

  override def userBought(user: UserId, ad: AdId): F[Unit] =
    repo.userBought(user, ad)

  override def userDiscussed(user: UserId, ad: AdId): F[Unit] =
    repo.userDiscussed(user, ad)
}

object TelemetryServiceInterpreter {
  def make[F[_]](repo: TelemetryRepository[F]): TelemetryService[F] =
    new TelemetryServiceInterpreter(repo)
}