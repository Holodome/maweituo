package maweituo.it.resources

import cats.effect.{IO, Resource}

import maweituo.tests.containers.makeMinioResource

import io.minio.MinioAsyncClient
import org.typelevel.log4cats.*
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.{GlobalRead, GlobalResource, GlobalWrite}

object MinioContainerResource extends GlobalResource:

  final case class MinioCon(value: MinioAsyncClient)

  override def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    given Logger[IO] = NoOpLogger[IO]
    for
      pg <- makeMinioResource[IO].map(MinioCon.apply)
      _  <- global.putR(pg)
    yield ()

extension (global: GlobalRead)
  def minio: Resource[IO, MinioAsyncClient] =
    global.getR[MinioContainerResource.MinioCon]().flatMap {
      case Some(value) => Resource.pure(value.value)
      case None        => makeMinioResource[IO]
    }
