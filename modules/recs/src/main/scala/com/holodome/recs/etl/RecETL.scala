package com.holodome.recs.etl

import cats.syntax.all._
import cats.Monad
import com.holodome.infrastructure.{GenObjectStorageId, ObjectStorage}
import com.holodome.recs.domain.recommendations.OBSSnapshotLocations

trait RecETLExtractor[F[_]] {
  def extract(locs: OBSSnapshotLocations, obs: ObjectStorage[F]): F[Unit]
}

trait RecETLLoader[F[_]] {
  def load(locs: OBSSnapshotLocations, obs: ObjectStorage[F]): F[Unit]
}

trait RecETL[F[_]] {
  def run: F[Unit]
}

object RecETL {
  def make[F[_]: Monad: GenObjectStorageId](
      extractor: RecETLExtractor[F],
      loader: RecETLLoader[F],
      obs: ObjectStorage[F]
  ): RecETL[F] =
    new RecETLInterpreter(extractor, loader, obs)

  private final class RecETLInterpreter[F[_]: Monad: GenObjectStorageId](
      extractor: RecETLExtractor[F],
      loader: RecETLLoader[F],
      obs: ObjectStorage[F]
  ) extends RecETL[F] {
    override def run: F[Unit] = for {
      locs <- (
        GenObjectStorageId[F].make,
        GenObjectStorageId[F].make,
        GenObjectStorageId[F].make,
        GenObjectStorageId[F].make,
        GenObjectStorageId[F].make
      ).mapN(OBSSnapshotLocations.apply)
      _ <- extractor.extract(locs, obs)
      _ <- loader.load(locs, obs)
    } yield ()
  }
}
