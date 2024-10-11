package maweituo
package logic
package interp

import maweituo.domain.all.*
import maweituo.infrastructure.effects.Background

object TelemetryServiceBackgroundInterp:
  def make[F[_]: Functor: Background](
      internal: TelemetryService[F]
  ): TelemetryService[F] = new:
    def userViewed(user: UserId, ad: AdId): F[Unit] =
      runInBackground(internal.userViewed(user, ad))

    def userCreated(user: UserId, ad: AdId): F[Unit] =
      runInBackground(internal.userCreated(user, ad))

    def userBought(user: UserId, ad: AdId): F[Unit] =
      runInBackground(internal.userBought(user, ad))

    def userDiscussed(user: UserId, ad: AdId): F[Unit] =
      runInBackground(internal.userDiscussed(user, ad))

    private def runInBackground[A](fa: F[A]) =
      Background[F].schedule(fa)
