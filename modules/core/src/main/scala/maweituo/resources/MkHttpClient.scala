package maweituo
package resources

import cats.effect.{Async, Resource}

import maweituo.config.HttpClientConfig

import fs2.io.net.Network
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

trait MkHttpClient[F[_]]:
  def newClient(c: HttpClientConfig): Resource[F, Client[F]]

object MkHttpClient:
  def apply[F[_]: MkHttpClient]: MkHttpClient[F] = summon

  given [F[_]: Async: Network]: MkHttpClient[F] = new:

    def newClient(c: HttpClientConfig): Resource[F, Client[F]] =
      EmberClientBuilder
        .default[F]
        .withTimeout(c.timeout)
        .withIdleTimeInPool(c.idleTimeInPool)
        .withHttp2
        .build
