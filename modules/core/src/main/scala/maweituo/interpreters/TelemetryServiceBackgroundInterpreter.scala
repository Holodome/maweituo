package maweituo.interpreters

import maweituo.domain.ads.AdId
import maweituo.domain.services.TelemetryService
import maweituo.domain.users.UserId
import maweituo.effects.Background

import cats.Functor

object TelemetryServiceBackgroundInterpreter:
  def make[F[_]: Functor: Background](
      internal: TelemetryService[F]
  ): TelemetryService[F] = new:
    def userCreated(user: UserId, ad: AdId): F[Unit] =
      runInBackground(internal.userCreated(user, ad))

    def userBought(user: UserId, ad: AdId): F[Unit] =
      runInBackground(internal.userBought(user, ad))

    def userDiscussed(user: UserId, ad: AdId): F[Unit] =
      runInBackground(internal.userDiscussed(user, ad))

    private def runInBackground[A](fa: F[A]) =
      Background[F].schedule(fa)
