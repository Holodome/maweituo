package maweituo.resources

import maweituo.config.HttpClientConfig

import cats.effect.{Async, Resource}
import fs2.io.net.Network
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

trait MkHttpClient[F[_]]:
  def newEmber(c: HttpClientConfig): Resource[F, Client[F]]

object MkHttpClient:
  def apply[F[_]: MkHttpClient]: MkHttpClient[F] = summon

  given [F[_]: Async: Network]: MkHttpClient[F] = (c: HttpClientConfig) =>
    EmberClientBuilder
      .default[F]
      .withTimeout(c.timeout)
      .withIdleTimeInPool(c.idleTimeInPool)
      .withHttp2
      .build
