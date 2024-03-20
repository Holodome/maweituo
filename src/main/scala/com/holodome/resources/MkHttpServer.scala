package com.holodome.resources

import org.http4s.HttpApp
import cats.effect.{Async, Resource}
import com.comcast.ip4s.IpLiteralSyntax
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.defaults.Banner
import org.typelevel.log4cats.Logger

trait MkHttpServer[F[_]] {
  def newEmber(httpApp: HttpApp[F]): Resource[F, Server]
}

object MkHttpServer {
  def apply[F[_]: MkHttpServer]: MkHttpServer[F] = implicitly

  private def showEmberBanner[F[_]: Logger](s: Server): F[Unit] =
    Logger[F].info(
      s"\n${Banner.mkString("\n")}\nHTTP Server started at ${s.address}"
    )

  implicit def forAsyncLogger[F[_]: Async: Logger]: MkHttpServer[F] =
    (httpApp: HttpApp[F]) => EmberServerBuilder
      .default[F]
      .withHost(host"localhost")
      .withPort(port"8080")
      .withHttpApp(httpApp)
      .build
      .evalTap(showEmberBanner[F])
}
