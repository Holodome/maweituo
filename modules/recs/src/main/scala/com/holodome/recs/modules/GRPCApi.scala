package com.holodome.recs.modules

import cats.effect.Temporal
import cats.syntax.all._
import com.holodome.effects.GenUUID
import com.holodome.recs.grpc.{RecommendationGRPCServer, TelemetryGRPCServer}
import org.http4s.HttpApp

object GRPCApi {
  def make[F[_]: GenUUID: Temporal](services: Services[F]): GRPCApi[F] =
    new GRPCApi(services) {}
}

sealed class GRPCApi[F[_]: GenUUID: Temporal] private (services: Services[F]) {
  private val recs      = RecommendationGRPCServer.make[F](services.recs)
  private val telemetry = TelemetryGRPCServer.make[F](services.telemetry)

  val httpApp: HttpApp[F] = (recs <+> telemetry).orNotFound
}
