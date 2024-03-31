package com.holodome.services

import cats.Functor
import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId
import com.holodome.effects.Background

import scala.concurrent.duration.DurationInt

trait TelemetryService[F[_]] {
  def userClicked(user: UserId, ad: AdId): F[Unit]
  def userBought(user: UserId, ad: AdId): F[Unit]
  def userDiscussed(user: UserId, ad: AdId): F[Unit]
}

object TelemetryService {
  def makeBackground[F[_]: Functor: Background](
      internal: TelemetryService[F]
  ): TelemetryService[F] = new TelemetryServiceBackgroundInterpreter(internal)

  private final class TelemetryServiceBackgroundInterpreter[F[_]: Functor: Background](
      internal: TelemetryService[F]
  ) extends TelemetryService[F] {

    override def userClicked(user: UserId, ad: AdId): F[Unit] =
      runInBackground(internal.userClicked(user, ad))

    override def userBought(user: UserId, ad: AdId): F[Unit] =
      runInBackground(internal.userBought(user, ad))

    override def userDiscussed(user: UserId, ad: AdId): F[Unit] =
      runInBackground(internal.userDiscussed(user, ad))

    private def runInBackground[A](fa: F[A]) =
      Background[F].schedule(fa, 1.minute)

  }

}
