package com.holodome.recs.modules

import cats.effect.kernel.Async
import com.holodome.recs.repositories.{RecRepository, TelemetryRepository}
import com.holodome.recs.repositories.cassandra.{CassandraRecRepository, CassandraTelemetryRepository}
import com.ringcentral.cassandra4io.CassandraSession

sealed abstract class Repositories[F[_]] {
  val recs: RecRepository[F]
  val telemetry: TelemetryRepository[F]
}

object Repositories {
  def make[F[_]: Async](cassandra: CassandraSession[F]): Repositories[F] =
    new Repositories[F] {
      override val recs: RecRepository[F] = CassandraRecRepository.make[F](cassandra)
      override val telemetry: TelemetryRepository[F] =
        CassandraTelemetryRepository.make[F](cassandra)
    }

}
