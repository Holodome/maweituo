package com.holodome.tests.services.stubs

import com.holodome.domain.services.TelemetryService
import com.holodome.domain.{ ads, users }

import cats.Applicative

class TelemetryServiceStub[F[_]: Applicative] extends TelemetryService[F]:
  override def userCreated(user: users.UserId, ad: ads.AdId): F[Unit] = Applicative[F].unit

  override def userBought(user: users.UserId, ad: ads.AdId): F[Unit] = Applicative[F].unit

  override def userDiscussed(user: users.UserId, ad: ads.AdId): F[Unit] = Applicative[F].unit
