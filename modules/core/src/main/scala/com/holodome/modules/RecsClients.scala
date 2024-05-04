package com.holodome.modules

import cats.Applicative
import cats.MonadThrow
import cats.effect.Concurrent
import cats.syntax.all._
import com.holodome.config.types.RecsClientConfig
import com.holodome.domain.ads.AdId
import com.holodome.domain.users
import com.holodome.effects.GenUUID
import com.holodome.grpc.RecommendationGRPCClientInterpreter
import com.holodome.grpc.TelemetryGRPCClientInterpreter
import com.holodome.services.RecommendationService
import com.holodome.services.TelemetryService
import org.http4s.client.Client

sealed abstract class RecsClients[F[_]] {
  val recs: RecommendationService[F]
  val telemetry: TelemetryService[F]
}

object RecsClients {
  def make[F[_]: Concurrent: GenUUID: MonadThrow](
      client: Client[F],
      cfg: RecsClientConfig
  ): RecsClients[F] =
    if (cfg.noRecs) {
      makeStub
    } else {
      makeGRPC(client, cfg)
    }

  private def makeStub[F[_]: Applicative]: RecsClients[F] =
    new RecsClients[F] {
      override val recs: RecommendationService[F] = new RecommendationService[F] {

        override def getRecs(user: users.UserId, count: Int): F[List[AdId]] =
          List.empty[AdId].pure[F]

        override def learn: F[Unit] = Applicative[F].unit
      }
      override val telemetry: TelemetryService[F] = new TelemetryService[F] {

        override def userCreated(user: users.UserId, ad: AdId): F[Unit] = Applicative[F].unit

        override def userBought(user: users.UserId, ad: AdId): F[Unit] = Applicative[F].unit

        override def userDiscussed(user: users.UserId, ad: AdId): F[Unit] =
          Applicative[F].unit
      }
    }

  private def makeGRPC[F[_]: Concurrent: GenUUID](
      client: Client[F],
      cfg: RecsClientConfig
  ): RecsClients[F] =
    new RecsClients[F] {
      override val recs: RecommendationService[F] =
        RecommendationGRPCClientInterpreter.make[F](client, cfg.uri)
      override val telemetry: TelemetryService[F] =
        TelemetryGRPCClientInterpreter.make[F](client, cfg.uri)
    }
}
