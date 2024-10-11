package maweituo
package tests
package services
package stubs

import maweituo.domain.all.*

class TelemetryServiceStub[F[_]: Applicative] extends TelemetryService[F]:

  override def userViewed(user: UserId, ad: AdId): F[Unit] = Applicative[F].unit

  override def userCreated(user: UserId, ad: AdId): F[Unit] = Applicative[F].unit

  override def userBought(user: UserId, ad: AdId): F[Unit] = Applicative[F].unit

  override def userDiscussed(user: UserId, ad: AdId): F[Unit] = Applicative[F].unit
