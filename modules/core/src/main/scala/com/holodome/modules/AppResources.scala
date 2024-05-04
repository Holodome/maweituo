package com.holodome.modules

import cats.effect.Async
import cats.effect.Resource
import cats.syntax.all._
import com.holodome.config.types.AppConfig
import com.holodome.resources.MkCassandraClient
import com.holodome.resources.MkHttpClient
import com.holodome.resources.MkMinioClient
import com.holodome.resources.MkRedisClient
import com.ringcentral.cassandra4io.CassandraSession
import dev.profunktor.redis4cats.RedisCommands
import io.minio.MinioAsyncClient
import org.http4s.client.Client

sealed abstract class AppResources[F[_]](
    val redis: RedisCommands[F, String, String],
    val cassandra: CassandraSession[F],
    val minio: MinioAsyncClient,
    val grpcClient: Client[F]
)

object AppResources {
  def make[F[_]: MkRedisClient: MkMinioClient: MkCassandraClient: MkHttpClient: Async](
      cfg: AppConfig
  ): Resource[F, AppResources[F]] = {
    (
      MkRedisClient[F].newClient(cfg.redis),
      MkCassandraClient[F].newClient(cfg.cassandra),
      MkMinioClient[F].newClient(cfg.minio),
      MkHttpClient[F].newEmber(cfg.recs.client)
    )
      .parMapN(new AppResources[F](_, _, _, _) {})
  }
}
