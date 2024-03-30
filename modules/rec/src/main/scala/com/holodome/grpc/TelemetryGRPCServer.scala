package com.holodome.grpc

import cats.Monad
import com.holodome.effects.GenUUID
import cats.syntax.all._
import cats.data.OptionT
import cats.effect.Temporal
import com.holodome.domain.users.UserId
import com.holodome.domain.Id
import com.holodome.domain.ads.AdId
import com.holodome.services.TelemetryService
import org.http4s.{Headers, HttpRoutes}

object TelemetryGRPCServer {
  def make[F[_]: GenUUID: Temporal](service: TelemetryService[F]): HttpRoutes[F] =
    rec.TelemetryService.toRoutes(new TelemetryGRPCServer(service))
}

private final class TelemetryGRPCServer[F[_]: Monad: GenUUID](service: TelemetryService[F])
    extends rec.TelemetryService[F] {

  override def userClicked(request: rec.UserAdAction, ctx: Headers): F[rec.Empty] =
    act(request)(service.userClicked)

  override def userBought(request: rec.UserAdAction, ctx: Headers): F[rec.Empty] =
    act(request)(service.userBought)

  override def userDiscussed(request: rec.UserAdAction, ctx: Headers): F[rec.Empty] =
    act(request)(service.userDiscussed)

  private def act(request: rec.UserAdAction)(f: (UserId, AdId) => F[Unit]): F[rec.Empty] =
    userAdActionToDomain(request)
      .flatMap { case (userId, adId) =>
        f(userId, adId)
      }
      .map(_ => rec.Empty())

  private def userAdActionToDomain(request: rec.UserAdAction): F[(UserId, AdId)] =
    OptionT
      .fromOption((request.user.map(_.value), request.ad.map(_.value)).tupled)
      .getOrElse(rec.Empty())
      .flatMap { case (u: rec.UUID, a: rec.UUID) =>
        for {
          userId <- Id.read[F, UserId](u.value)
          adId   <- Id.read[F, AdId](a.value)
        } yield userId -> adId
      }
}
