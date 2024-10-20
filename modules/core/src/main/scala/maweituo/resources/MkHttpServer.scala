package maweituo
package resources

import cats.effect.{Async, Resource}

import maweituo.config.HttpServerConfig

import fs2.io.net.Network
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.defaults.Banner
import org.typelevel.log4cats.syntax.*
import org.typelevel.log4cats.{Logger, LoggerFactory}

trait MkHttpServer[F[_]]:
  def newClient(
      config: HttpServerConfig,
      httpApp: HttpApp[F]
  ): Resource[F, Server]

object MkHttpServer:
  def apply[F[_]: MkHttpServer]: MkHttpServer[F] = summon

  private def showEmberBanner[F[_]: Logger](s: Server): F[Unit] =
    info"\n${Banner.mkString("\n")}\nHTTP Server started at ${s.address}"

  given [F[_]: Async: LoggerFactory: Network]: MkHttpServer[F] =
    given Logger[F] = LoggerFactory[F].getLogger
    (config: HttpServerConfig, httpApp: HttpApp[F]) =>
      EmberServerBuilder
        .default[F]
        .withHost(config.host)
        .withPort(config.port)
        .withHttpApp(httpApp)
        .withHttp2
        .withoutTLS
        .build
        .evalTap(showEmberBanner[F])
