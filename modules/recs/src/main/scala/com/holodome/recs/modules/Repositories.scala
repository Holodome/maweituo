package com.holodome.recs.modules

import cats.effect.Async
import com.holodome.recs.cassandra.repositories.CassandraTelemetryRepository
import com.holodome.recs.clickhouse.repositories.ClickhouseRecRepository
import com.holodome.recs.domain.repositories.{RecRepository, TelemetryRepository}
import com.ringcentral.cassandra4io.CassandraSession
import doobie.util.transactor.Transactor

sealed abstract class Repositories[F[_]] {
  val recs: RecRepository[F]
  val telemetry: TelemetryRepository[F]
}

object Repositories {
  def make[F[_]: Async](
      cassandra: CassandraSession[F],
      clickhouse: Transactor[F]
  ): Repositories[F] =
    new Repositories[F] {
      override val recs: RecRepository[F] = ClickhouseRecRepository.make[F](clickhouse)
      override val telemetry: TelemetryRepository[F] =
        CassandraTelemetryRepository.make[F](cassandra)
    }

}
