package maweituo
package tests
package e2e
package resources

import java.io.File

import scala.concurrent.duration.DurationInt
import scala.io.Source

import maweituo.config.{HttpClientConfig, PostgresConfig}
import maweituo.resources.{MkHttpClient, MkPostgresClient}

import com.comcast.ip4s.*
import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import doobie.implicits.*
import doobie.util.fragment.Fragment
import org.http4s.client.Client
import weaver.{GlobalRead, GlobalResource, GlobalWrite}

final case class AppCon(client: Client[IO], base: String)

object AppResource extends GlobalResource:

  private val appComposeDef = DockerComposeContainer.Def(
    composeFiles =
      DockerComposeContainer.ComposeFile(Left(new File("modules/e2e/src/test/resources/docker-compose.yml"))),
    exposedServices = List(
      ExposedService("core", 8080),
      ExposedService("postgres", 5432)
    )
  )

  def appResource: Resource[IO, AppCon] =
    Resource.eval(IO.delay(appComposeDef.start()))
      .evalTap { container =>
        val pgHost = Host.fromString(container.getServiceHost("postgres", 5432)).get
        val pgPort = Port.fromInt(container.getServicePort("postgres", 5432)).get
        val cfg    = PostgresConfig("maweituo", "maweituo", pgHost, pgPort, "maweituo")
        MkPostgresClient[IO].newClient(cfg).use {
          xa =>
            Fragment.const(Source.fromFile("deploy/init.sql").mkString)
              .update.run.transact(xa).void
        }
      }.flatMap { container =>
        val host = container.getServiceHost("core", 8080)
        val port = container.getServicePort("core", 8080)
        MkHttpClient[IO].newClient(HttpClientConfig(5.seconds, 5.seconds))
          .map(x => (x, s"http://$host:$port"))
      }.map(AppCon.apply)

  override def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    for
      x <- appResource
      _ <- global.putR(x)
    yield ()

extension (global: GlobalRead)
  def app: Resource[IO, AppCon] =
    global.getR[AppCon]().flatMap {
      case Some(value) => Resource.pure(value)
      case None        => AppResource.appResource
    }
