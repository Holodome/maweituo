package maweituo
package resources

import cats.effect.{Async, Resource}

import maweituo.config.HttpClientConfig

import fs2.io.net.Network
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.Logger

trait MkHttpClient[F[_]]:
  def newClient(c: HttpClientConfig): Resource[F, Client[F]]
  def newClientWithLog(c: HttpClientConfig, logger: Logger[F]): Resource[F, Client[F]]

object MkHttpClient:
  def apply[F[_]: MkHttpClient]: MkHttpClient[F] = summon

  given [F[_]: Async: Network]: MkHttpClient[F] = new:

    private def builder(c: HttpClientConfig) =
      EmberClientBuilder
        .default[F]
        .withTimeout(c.timeout)
        .withIdleTimeInPool(c.idleTimeInPool)

    def newClientWithLog(c: HttpClientConfig, logger: Logger[F]): Resource[F, Client[F]] =
      builder(c).withLogger(logger)
        .withHttp2
        .build

    def newClient(c: HttpClientConfig): Resource[F, Client[F]] =
      builder(c)
        .withHttp2
        .build
