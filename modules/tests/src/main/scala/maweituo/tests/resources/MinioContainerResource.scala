package maweituo.tests.resources

import cats.effect.{IO, Resource}

import maweituo.infrastructure.minio.MinioConnection
import maweituo.tests.containers.makeMinioResource

import weaver.{GlobalRead, GlobalResource, GlobalWrite}

final case class MinioCon(value: MinioConnection)

trait MinioContainerResource:
  this: GlobalResource =>

  override def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    for
      minio <- makeMinioResource[IO].map(MinioCon.apply)
      _     <- global.putR(minio)
    yield ()

extension (global: GlobalRead)
  def minio: Resource[IO, MinioConnection] =
    global.getR[MinioCon]().flatMap {
      case Some(value) => Resource.pure(value.value)
      case None        => makeMinioResource[IO]
    }
