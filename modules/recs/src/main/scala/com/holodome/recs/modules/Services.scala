package com.holodome.recs.modules

import cats.syntax.all._
import cats.effect.std.Random
import cats.Monad
import cats.effect.Sync
import com.holodome.recs.services.{RecommendationService, TelemetryService}

sealed abstract class Services[F[_]] {
  val telemetry: TelemetryService[F]
  val recs: RecommendationService[F]
}

object Services {

  def make[F[_]: Sync]: F[Services[F]] = {
    Random.javaUtilRandom[F](new java.util.Random).map { implicit rng =>
      new Services[F] {
        override val telemetry: TelemetryService[F] = TelemetryService.make[F]
        override val recs: RecommendationService[F] = RecommendationService.make[F](???, ???)
      }
    }
  }

}
