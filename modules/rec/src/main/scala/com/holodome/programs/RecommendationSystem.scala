package com.holodome.programs

import cats.syntax.all._
import cats.MonadThrow
import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId
import com.holodome.services.{RecommendationService, TelemetryService}

trait RecommendationSystem[F[_]] {
  def getRecommendations(userId: UserId): F[List[AdId]]
  def learn(): F[Unit]
}

object RecommendationSystem {
  def make[F[_]: MonadThrow](
      rec: RecommendationService[F],
  ): RecommendationSystem[F] =
    new RecommendationSystemInterpreter(rec)

  private class RecommendationSystemInterpreter[F[_]: MonadThrow](
      rec: RecommendationService[F],
  ) extends RecommendationSystem[F] {

    override def getRecommendations(userId: UserId): F[List[AdId]] =
      rec.getRecs(userId)

    override def learn(): F[Unit] = {
      for {
        _ <- rec.updateDbSnapshot()
        _ <- rec.storeTelemetry()
      } yield ()
    }

  }
}
