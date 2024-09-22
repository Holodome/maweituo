package com.holodome.interpreters

import com.holodome.domain.ads.AdId
import com.holodome.domain.services.TelemetryService
import com.holodome.domain.users.UserId
import com.holodome.effects.Background

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
