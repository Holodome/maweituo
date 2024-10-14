package maweituo
package tests
package resources

import maweituo.tests.containers.{PartiallyAppliedPostgres, makePostgresResource}

import weaver.{GlobalRead, GlobalResource, GlobalWrite}

trait PostgresContainerResource:
  this: GlobalResource =>

  override def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    for
      pg <- makePostgresResource
      _  <- global.putR(pg)
    yield ()

extension (global: GlobalRead)
  def postgres: Resource[IO, PartiallyAppliedPostgres] =
    global.getR[PartiallyAppliedPostgres]().flatMap {
      case Some(value) => Resource.pure(value)
      case None        => makePostgresResource
    }
