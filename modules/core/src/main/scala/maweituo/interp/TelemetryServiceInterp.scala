package maweituo.interp

import cats.Monad
import cats.syntax.all.*

import maweituo.domain.ads.AdId
import maweituo.domain.repos.TelemetryRepo
import maweituo.domain.services.TelemetryService
import maweituo.domain.users.UserId
import maweituo.effects.TimeSource

object TelemetryServiceInterp:
  def make[F[_]: TimeSource: Monad](tel: TelemetryRepo[F]): TelemetryService[F] = new:
    def userViewed(user: UserId, ad: AdId): F[Unit] =
      for
        at <- TimeSource[F].instant
        _  <- tel.userViewed(user, ad, at)
      yield ()

    def userCreated(user: UserId, ad: AdId): F[Unit] =
      for
        at <- TimeSource[F].instant
        _  <- tel.userCreated(user, ad, at)
      yield ()

    def userBought(user: UserId, ad: AdId): F[Unit] =
      for
        at <- TimeSource[F].instant
        _  <- tel.userBought(user, ad, at)
      yield ()

    def userDiscussed(user: UserId, ad: AdId): F[Unit] =
      for
        at <- TimeSource[F].instant
        _  <- tel.userDiscussed(user, ad, at)
      yield ()
