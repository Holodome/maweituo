package maweituo
package modules

import cats.effect.Async

import maweituo.http.*
import maweituo.http.endpoints.all.*

import org.http4s.HttpApp
import org.http4s.implicits.*
import org.typelevel.log4cats.LoggerFactory
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object HttpApi:
  def make[F[_]: Async: LoggerFactory](services: Services[F]): HttpApi[F] =
    new HttpApi[F](services)

sealed class HttpApi[F[_]: Async: LoggerFactory](
    services: Services[F]
):

  private lazy val endpoints =
    val routesBuilder = new RoutesBuilder[F](services.auth)
    List(
      AuthEndpoints[F](services.auth, routesBuilder),
      RegisterEndpoints[F](services.users, routesBuilder),
      AdEndpoints[F](services.ads, routesBuilder),
      AdChatEndpoints[F](services.chats, routesBuilder),
      AdImageEndpoints[F](services.images, routesBuilder),
      AdMsgEndpoints[F](services.messages, routesBuilder),
      AdTagEndpoints[F](services.tags, routesBuilder),
      UserEndpoints[F](services.users, routesBuilder),
      UserAdEndpoints[F](services.userAds, routesBuilder),
      TagEndpoints[F](services.tags, routesBuilder),
      FeedEndpoints[F](services.feed, routesBuilder)
    ).map(_.endpoints).flatten

  private lazy val allEndpoints =
    val swaggerEndpoints = SwaggerInterpreter().fromServerEndpoints[F](endpoints, "maweituo", "0.1")
    endpoints ++ swaggerEndpoints

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
