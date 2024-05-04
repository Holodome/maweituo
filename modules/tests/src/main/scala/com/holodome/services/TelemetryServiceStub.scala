package com.holodome.services

import cats.Applicative
import com.holodome.domain.ads
import com.holodome.domain.users
import com.holodome.services.TelemetryService

class TelemetryServiceStub[F[_]: Applicative] extends TelemetryService[F] {

  override def userCreated(user: users.UserId, ad: ads.AdId): F[Unit] = Applicative[F].unit

  override def userBought(user: users.UserId, ad: ads.AdId): F[Unit] = Applicative[F].unit

  override def userDiscussed(user: users.UserId, ad: ads.AdId): F[Unit] = Applicative[F].unit
}
