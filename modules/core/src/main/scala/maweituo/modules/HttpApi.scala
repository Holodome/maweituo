package maweituo
package modules

import cats.effect.Async

import maweituo.config.AppConfig
import maweituo.http.*
import maweituo.http.endpoints.all.*

import org.http4s.HttpApp
import org.http4s.implicits.*
import org.typelevel.log4cats.LoggerFactory
import sttp.apispec.openapi.Server
import sttp.apispec.openapi.circe.yaml.*
import sttp.tapir.*
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.swagger.SwaggerUI

object HttpApi:
  def make[F[_]: Async: LoggerFactory](cfg: AppConfig, services: Services[F]): HttpApi[F] =
    new HttpApi[F](cfg, services)

sealed class HttpApi[F[_]: Async: LoggerFactory](
    cfg: AppConfig,
    services: Services[F]
):

  private lazy val endpoints =
    given EndpointsBuilder[F] = EndpointsBuilder[F](services.auth)
    List(
      AuthEndpoints[F](services.auth),
      RegisterEndpoints[F](services.users),
      AdEndpoints[F](services.ads),
      AdChatEndpoints[F](services.chats),
      AdImageEndpoints[F](services.images),
      AdMsgEndpoints[F](services.messages),
      AdTagEndpoints[F](services.tags),
      UserEndpoints[F](services.users),
      UserAdEndpoints[F](services.userAds),
      UserChatEndpoints[F](services.userChats),
      TagEndpoints[F](services.tags),
      FeedEndpoints[F](services.feed)
    ).map(_.endpoints).flatten

  private lazy val allEndpoints =
    val swaggerEndpoints = OpenAPIDocsInterpreter().serverEndpointsToOpenAPI(endpoints, "My App", "1.0")
      .servers(
        List(Server(s"http://127.0.0.1:${cfg.httpServer.port}").description("Production server"))
      )
      .toYaml
    endpoints ++ SwaggerUI[F](swaggerEndpoints)

  private val serverOptions: Http4sServerOptions[F] = Http4sServerOptions
    .customiseInterceptors[F]
    .defaultHandlers(
      msg => ValuedEndpointOutput(jsonBody[ErrorResponseDto], ErrorResponseDto.make(msg)),
      notFoundWhenRejected = true
    )
    .serverLog {
      val l = LoggerFactory[F].getLogger
      Http4sServerOptions
        .defaultServerLog[F]
        .doLogWhenHandled((msg, e) => e.fold(l.info(msg))(l.info(_)(msg)))
        .doLogAllDecodeFailures((msg, e) => e.fold(l.info(msg))(l.info(_)(msg)))
        .doLogExceptions((msg, e) => l.error(e)(msg))
        .doLogWhenReceived(msg => l.info(msg))
    }
    .corsInterceptor(CORSInterceptor.default[F])
    .options

  private lazy val routes = Http4sServerInterpreter(serverOptions).toRoutes(allEndpoints)

  lazy val httpApp: HttpApp[F] = routes.orNotFound
