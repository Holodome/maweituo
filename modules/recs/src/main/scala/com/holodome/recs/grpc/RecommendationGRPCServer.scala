package com.holodome.recs.grpc

import cats.syntax.all._
import cats.{Applicative, Monad}
import cats.data.OptionT
import cats.effect.Temporal
import com.holodome.domain.users.UserId
import com.holodome.domain.Id
import com.holodome.effects.GenUUID
import com.holodome.proto
import com.holodome.recs.services.RecommendationService
import org.http4s.{Headers, HttpRoutes}

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
    service.learn().map(_ => proto.rec.Empty())

  override def getRecs(request: proto.rec.UserId, ctx: Headers): F[proto.rec.Recommendations] = {
    OptionT
      .fromOption(request.value)
      .fold(Applicative[F].pure(proto.rec.Recommendations(Seq()))) { uuid =>
        for {
          id     <- Id.read[F, UserId](uuid.value)
          result <- service.getRecs(id, 10)
        } yield proto.rec.Recommendations(
          result.map(adId => proto.rec.AdId.apply(Some(proto.rec.UUID(adId.value.toString))))
        )
      }
      .flatten
  }
}
