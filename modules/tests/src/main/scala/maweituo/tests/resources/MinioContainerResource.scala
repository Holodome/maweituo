package maweituo
package tests
package resources
import maweituo.tests.containers.{PartiallyAppliedMinio, makeMinioResource}

import weaver.{GlobalRead, GlobalResource, GlobalWrite}

trait MinioContainerResource:
  this: GlobalResource =>

  override def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    for
      minio <- makeMinioResource
      _     <- global.putR(minio)
    yield ()

extension (global: GlobalRead)
  def minio: Resource[IO, PartiallyAppliedMinio] =
    global.getR[PartiallyAppliedMinio]().flatMap {
      case Some(value) => Resource.pure(value)
      case None        => makeMinioResource
    }
