package com.holodome.grpc

import cats.syntax.all._
import cats.{Applicative, Monad}
import cats.data.OptionT
import cats.effect.Temporal
import com.holodome.domain.users.UserId
import com.holodome.domain.Id
import com.holodome.effects.GenUUID
import com.holodome.services.RecommendationService
import org.http4s.{Headers, HttpRoutes}

object RecommendationGRPCServer {
  def make[F[_]: GenUUID: Temporal](
      service: RecommendationService[F]
  ): HttpRoutes[F] =
    rec.RecommendationService.toRoutes[F](new RecommendationGRPCServer(service))
}

private final class RecommendationGRPCServer[F[_]: Monad: GenUUID](
    service: RecommendationService[F]
) extends rec.RecommendationService[F] {

  override def learn(request: rec.Empty, ctx: Headers): F[rec.Empty] =
    service.learn().map(_ => rec.Empty())

  override def getRecs(request: rec.UserId, ctx: Headers): F[rec.Recommendations] = {
    OptionT
      .fromOption(request.value)
      .fold(Applicative[F].pure(rec.Recommendations(Seq()))) { uuid =>
        for {
          id     <- Id.read[F, UserId](uuid.value)
          result <- service.getRecs(id, 10)
        } yield rec.Recommendations(
          result.map(adId => rec.AdId.apply(Some(rec.UUID(adId.value.toString))))
        )
      }
      .flatten
  }
}
