package maweituo.e2e.resources

import java.io.File

import scala.concurrent.duration.DurationInt
import scala.io.Source

import cats.effect.IO
import cats.effect.kernel.Resource

import maweituo.config.{HttpClientConfig, PostgresConfig}
import maweituo.e2e.AppClient
import maweituo.resources.{MkHttpClient, MkPostgresClient}

import com.comcast.ip4s.*
import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import doobie.implicits.*
import doobie.util.fragment.Fragment
import org.http4s.implicits.*
import weaver.{GlobalRead, GlobalResource, GlobalWrite}

object AppResource extends GlobalResource:

  private val appComposeDef = DockerComposeContainer.Def(
    composeFiles = DockerComposeContainer.ComposeFile(Left(new File("src/test/resources/docker-compose.yml"))),
    exposedServices = List(
      ExposedService("core", 8080),
      ExposedService("postgres", 5432)
    )
  )

  def appResource: Resource[IO, AppClient] =
    Resource.pure(appComposeDef.start())
      .flatMap { container =>
        MkPostgresClient[IO]
          .newClient(PostgresConfig("maweituo", "maweituo", host"localhost", port"5432", "maweituo"))
      }
      .evalTap { xa =>
        Fragment.const(Source.fromResource("init.sql").mkString)
          .update.run.transact(xa).void
      }.flatMap { _ =>
        MkHttpClient[IO].newEmber(HttpClientConfig(5.seconds, 5.seconds))
      }.map { client =>
        new AppClient(uri"http://localhost:8080", client)
      }

  override def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    for
      x <- appResource
      _ <- global.putR(x)
    yield ()

extension (global: GlobalRead)
  def redis: Resource[IO, AppClient] =
    global.getR[AppClient]().flatMap {
      case Some(value) => Resource.pure(value)
      case None        => AppResource.appResource
    }
