package maweituo.tests.services.stubs

import cats.Applicative

import maweituo.domain.ads.AdId
import maweituo.domain.services.TelemetryService
import maweituo.domain.users.UserId
import maweituo.domain.{ads, users}

class TelemetryServiceStub[F[_]: Applicative] extends TelemetryService[F]:

  override def userViewed(user: UserId, ad: AdId): F[Unit] = Applicative[F].unit

  override def userCreated(user: users.UserId, ad: ads.AdId): F[Unit] = Applicative[F].unit

  override def userBought(user: users.UserId, ad: ads.AdId): F[Unit] = Applicative[F].unit

  override def userDiscussed(user: users.UserId, ad: ads.AdId): F[Unit] = Applicative[F].unit
