package maweituo.it.resources

import weaver.GlobalResource
import weaver.GlobalWrite

import cats.effect.IO
import cats.effect.Resource
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import maweituo.tests.containers.makePostgresResource
import doobie.util.transactor.Transactor

object PostgresContainerResource extends GlobalResource:
  
  final case class PgCon(xa: Transactor[IO])

  override def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    given Logger[IO] = NoOpLogger[IO]
    for
      pg <- makePostgresResource[IO].map(PgCon.apply)
      _  <- global.putR(pg)
    yield ()
