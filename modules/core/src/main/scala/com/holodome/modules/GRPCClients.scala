package com.holodome.modules

import cats.effect.Concurrent
import cats.MonadThrow
import com.comcast.ip4s.SocketAddress
import com.holodome.config.types.GrpcConfig
import com.holodome.effects.GenUUID
import com.holodome.grpc.{TelemetryGRPCClientInterpreter, RecommendationGRPCClientInterpreter}
import com.holodome.services.{RecommendationService, TelemetryService}
import org.http4s.client.Client
import org.http4s.Uri

sealed abstract class GRPCClients[F[_]] {
  val recs: RecommendationService[F]
  val telemetry: TelemetryService[F]
}

object GRPCClients {
  def make[F[_]: Concurrent: GenUUID: MonadThrow](
      client: Client[F],
      cfg: GrpcConfig
  ): GRPCClients[F] = {
    val uri = Uri.unsafeFromString(SocketAddress(cfg.host, cfg.port).toString())
    new GRPCClients[F] {
      override val recs: RecommendationService[F] =
        RecommendationGRPCClientInterpreter.make[F](client, uri)
      override val telemetry: TelemetryService[F] =
        TelemetryGRPCClientInterpreter.make[F](client, uri)
    }
  }
}
