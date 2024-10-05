package maweituo.modules

import cats.effect.Concurrent
import cats.syntax.all.*
import cats.{Applicative, MonadThrow}

import maweituo.domain.ads.AdId
import maweituo.domain.services.{RecommendationService, TelemetryService}
import maweituo.domain.users
import maweituo.effects.GenUUID

sealed abstract class RecsClients[F[_]]:
  val recs: RecommendationService[F]
  val telemetry: TelemetryService[F]

object RecsClients:
  def make[F[_]: Concurrent: GenUUID: MonadThrow](
  ): RecsClients[F] =
    makeStub

  private def makeStub[F[_]: Applicative]: RecsClients[F] =
    new RecsClients[F]:
      override val recs: RecommendationService[F] = new RecommendationService[F]:

        override def getRecs(user: users.UserId, count: Int): F[List[AdId]] =
          List.empty[AdId].pure[F]

        override def learn: F[Unit] = Applicative[F].unit

      override val telemetry: TelemetryService[F] = new TelemetryService[F]:

        override def userCreated(user: users.UserId, ad: AdId): F[Unit] = Applicative[F].unit

        override def userBought(user: users.UserId, ad: AdId): F[Unit] = Applicative[F].unit

        override def userDiscussed(user: users.UserId, ad: AdId): F[Unit] =
          Applicative[F].unit