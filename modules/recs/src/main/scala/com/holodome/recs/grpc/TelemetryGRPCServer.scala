package com.holodome.recs.grpc

import cats.Monad
import cats.data.OptionT
import cats.effect.Temporal
import cats.syntax.all._
import com.holodome.domain.Id
import com.holodome.domain.ads.AdId
import com.holodome.domain.services.TelemetryService
import com.holodome.domain.users.UserId
import com.holodome.effects.GenUUID
import com.holodome.proto
import org.http4s.{Headers, HttpRoutes}

object TelemetryGRPCServer {
  def make[F[_]: GenUUID: Temporal](service: TelemetryService[F]): HttpRoutes[F] =
    proto.rec.TelemetryService.toRoutes(new TelemetryGRPCServer(service))
}

private final class TelemetryGRPCServer[F[_]: Monad: GenUUID](service: TelemetryService[F])
    extends proto.rec.TelemetryService[F] {

  override def userCreated(request: proto.rec.UserAdAction, ctx: Headers): F[proto.rec.Empty] =
    act(request)(service.userCreated)

  override def userBought(request: proto.rec.UserAdAction, ctx: Headers): F[proto.rec.Empty] =
    act(request)(service.userBought)

  override def userDiscussed(request: proto.rec.UserAdAction, ctx: Headers): F[proto.rec.Empty] =
    act(request)(service.userDiscussed)

  private def act(
      request: proto.rec.UserAdAction
  )(f: (UserId, AdId) => F[Unit]): F[proto.rec.Empty] =
    userAdActionToDomain(request)
      .flatMap { case (userId, adId) =>
        f(userId, adId)
      }
      .as(proto.rec.Empty())

  private def userAdActionToDomain(request: proto.rec.UserAdAction): F[(UserId, AdId)] =
    OptionT
      .fromOption((request.user.map(_.value), request.ad.map(_.value)).tupled)
      .getOrElse(proto.rec.Empty())
      .flatMap { case (u: String, a: String) =>
        for {
          userId <- Id.read[F, UserId](u)
          adId   <- Id.read[F, AdId](a)
        } yield userId -> adId
      }
}
