package com.holodome.grpc

import cats.syntax.all._
import cats.Monad
import cats.effect.Concurrent
import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId
import com.holodome.effects.GenUUID
import com.holodome.proto
import com.holodome.services.TelemetryService
import org.http4s.{Headers, Uri}
import org.http4s.client.Client

object TelemetryGRPCClientInterpreter {
  def make[F[_]: Concurrent: GenUUID](client: Client[F], uri: Uri): TelemetryService[F] =
    new TelemetryGRPCClientInterpreter(proto.rec.TelemetryService.fromClient(client, uri))
}

private final class TelemetryGRPCClientInterpreter[F[_]: Monad: GenUUID](
    rpc: proto.rec.TelemetryService[F]
) extends TelemetryService[F] {

  override def userClicked(user: UserId, ad: AdId): F[Unit] =
    rpc.userClicked(toRpcReq(user, ad), Headers.empty).map(_ => ())

  override def userBought(user: UserId, ad: AdId): F[Unit] =
    rpc.userBought(toRpcReq(user, ad), Headers.empty).map(_ => ())

  override def userDiscussed(user: UserId, ad: AdId): F[Unit] =
    rpc.userDiscussed(toRpcReq(user, ad), Headers.empty).map(_ => ())

  private def toRpcReq(user: UserId, ad: AdId): proto.rec.UserAdAction =
    proto.rec.UserAdAction(
      Some(proto.rec.UserId(user.value.toString)),
      Some(proto.rec.AdId(ad.value.toString))
    )
}
