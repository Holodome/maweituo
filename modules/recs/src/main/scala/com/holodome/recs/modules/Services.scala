package com.holodome.recs.modules

import cats.effect.Sync
import cats.syntax.all._
import com.holodome.domain.services.{RecommendationService, TelemetryService}
import com.holodome.effects.{Background, MkRandom}
import com.holodome.recs.etl.RecETL
import com.holodome.recs.services.{RecommendationServiceInterpreter, TelemetryServiceInterpreter}
import org.typelevel.log4cats.Logger

sealed abstract class Services[F[_]] {
  val telemetry: TelemetryService[F]
  val recs: RecommendationService[F]
}

object Services {

  def make[F[_]: Sync: MkRandom: Background: Logger](
      repositories: Repositories[F],
      etl: RecETL[F]
  ): F[Services[F]] = {
    MkRandom[F].make.map { implicit rng =>
      new Services[F] {
        override val telemetry: TelemetryService[F] =
          TelemetryServiceInterpreter.make[F](repositories.telemetry)
        override val recs: RecommendationService[F] =
          RecommendationServiceInterpreter.make[F](repositories.recs, etl)
      }
    }
  }

}
