package com.holodome.recs.grpc

import cats.Monad
import cats.effect.Temporal
import cats.syntax.all._
import com.holodome.domain.Id
import com.holodome.domain.users.UserId
import com.holodome.effects.GenUUID
import com.holodome.proto
import com.holodome.services.RecommendationService
import org.http4s.Headers
import org.http4s.HttpRoutes

object RecommendationGRPCServer {
  def make[F[_]: GenUUID: Temporal](
      service: RecommendationService[F]
  ): HttpRoutes[F] =
    proto.rec.RecommendationService.toRoutes[F](new RecommendationGRPCServer(service))
}

private final class RecommendationGRPCServer[F[_]: Monad: GenUUID](
    service: RecommendationService[F]
) extends proto.rec.RecommendationService[F] {

  override def learn(request: proto.rec.Empty, ctx: Headers): F[proto.rec.Empty] =
    service.learn.as(proto.rec.Empty())

  override def getRecs(request: proto.rec.UserId, ctx: Headers): F[proto.rec.Recommendations] =
    for {
      id     <- Id.read[F, UserId](request.value)
      result <- service.getRecs(id, 10)
    } yield proto.rec
      .Recommendations(
        result.map(adId => proto.rec.AdId(adId.value.toString))
      )
}
