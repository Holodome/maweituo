package com.holodome

import cats.effect.{ContextShift, IO, Timer}
import cats.implicits.toSemigroupKOps
import com.holodome.config.Config
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, Logger}
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.json.circe.jsonBody
import sttp.tapir._
import sttp.tapir.openapi.circe.yaml.RichOpenAPI
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt

object Server {
  private val endpointList
      : List[ServerEndpoint[Unit, String, String, Any, IO]] = List(
    endpoint.get
      .in("hello")
      .errorOut(stringBody)
      .out(jsonBody[String])
      .serverLogic(_ => IO { Right("hello").withLeft[String] })
  )

  def run(
      config: Config
  )(implicit timer: Timer[IO], contextShift: ContextShift[IO]) = {
    val openapi: OpenAPI = OpenAPIDocsInterpreter().serverEndpointsToOpenAPI(
      endpointList,
      "maweituo",
      "0.1"
    )

    val cors = CORS.policy.withAllowOriginAll.withAllowMethodsAll
      .withAllowCredentials(true)
      .withMaxAge(1.day)

    val openapiRoute: HttpRoutes[IO] =
      new SwaggerHttp4s(openapi.toYaml).routes[IO]
    val routes: HttpRoutes[IO] = cors(
      endpointList.foldLeft(openapiRoute)((r, s) =>
        r <+> Http4sServerInterpreter[IO].toRoutes(s)
      )
    )
    val httpApp =
      Logger.httpApp(logHeaders = true, logBody = false)(routes.orNotFound)
    BlazeServerBuilder[IO](global)
      .bindHttp(config.port, "localhost")
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
  }
}
