package com.holodome.grpc

import cats.syntax.all._
import cats.effect.Concurrent
import com.holodome.domain.Id
import cats.Monad
import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId
import com.holodome.effects.GenUUID
import org.http4s.client.Client
import org.http4s.{Headers, Uri}
import com.holodome.proto
import com.holodome.services.RecommendationService

object RecommendationGRPCClient {
  def make[F[_]: Concurrent: GenUUID](client: Client[F], uri: Uri): RecommendationService[F] =
    new RecommendationGRPCClientInterpreter(proto.rec.RecommendationService.fromClient(client, uri))

  private final class RecommendationGRPCClientInterpreter[F[_]: Monad: GenUUID](
      rpc: proto.rec.RecommendationService[F]
  ) extends RecommendationService[F] {
    override def learn(): F[Unit] = rpc.learn(proto.rec.Empty(), Headers.empty).map(_ => ())

    override def getRecs(userId: UserId, count: Int): F[List[AdId]] =
      rpc
        .getRecs(proto.rec.UserId(userId.value.toString), Headers.empty)
        .flatMap { recs =>
          recs.ads.map(_.value).toList.traverse(id => Id.read[F, AdId](id))
        }
  }
}
