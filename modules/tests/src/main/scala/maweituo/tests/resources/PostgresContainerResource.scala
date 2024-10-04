package maweituo.tests.resources

import cats.effect.{IO, Resource}

import maweituo.tests.containers.makePostgresResource

import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.{GlobalRead, GlobalResource, GlobalWrite}

final case class PgCon(xa: Transactor[IO])

trait PostgresContainerResource:
  this: GlobalResource =>

  override def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    given Logger[IO] = NoOpLogger[IO]
    for
      pg <- makePostgresResource[IO].map(PgCon.apply)
      _  <- global.putR(pg)
    yield ()

extension (global: GlobalRead)
  def postgres: Resource[IO, Transactor[IO]] =
    given Logger[IO] = NoOpLogger[IO]
    global.getR[PgCon]().flatMap {
      case Some(value) => Resource.pure(value.xa)
      case None        => makePostgresResource[IO]
    }
