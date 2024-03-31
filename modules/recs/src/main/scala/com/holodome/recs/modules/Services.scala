package com.holodome.recs.modules

import cats.syntax.all._
import cats.effect.Sync
import com.holodome.effects.MkRandom
import com.holodome.recs.services.{RecommendationService, TelemetryService}

sealed abstract class Services[F[_]] {
  val telemetry: TelemetryService[F]
  val recs: RecommendationService[F]
}

object Services {

  def make[F[_]: Sync: MkRandom](repositories: Repositories[F]): F[Services[F]] = {
    MkRandom[F].make.map { implicit rng =>
      new Services[F] {
        override val telemetry: TelemetryService[F] =
          TelemetryService.make[F](repositories.telemetry)
        override val recs: RecommendationService[F] =
          RecommendationService.make[F](???, repositories.recs, ???)
      }
    }
  }

}
