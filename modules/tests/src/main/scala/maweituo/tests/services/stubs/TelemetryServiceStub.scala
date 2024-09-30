package maweituo.tests.services.stubs

import maweituo.domain.ads.AdId
import maweituo.domain.services.TelemetryService
import maweituo.domain.{ads, users}

import cats.Applicative

class TelemetryServiceStub[F[_]: Applicative] extends TelemetryService[F]:
  override def userCreated(user: users.UserId, ad: ads.AdId): F[Unit] = Applicative[F].unit

  override def userBought(user: users.UserId, ad: ads.AdId): F[Unit] = Applicative[F].unit

  override def userDiscussed(user: users.UserId, ad: ads.AdId): F[Unit] = Applicative[F].unit
