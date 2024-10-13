package maweituo
package modules
import cats.Parallel
import cats.effect.Async

import maweituo.http.*
import maweituo.http.routes.all.*

import org.http4s.HttpApp
import org.http4s.implicits.*
import org.typelevel.log4cats.Logger
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object HttpApi:
  def make[F[_]: Async: Logger: Parallel](services: Services[F]): HttpApi[F] =
    new HttpApi[F](services)

sealed class HttpApi[F[_]: Async: Logger: Parallel](
    services: Services[F]
):

  private lazy val endpoints =
    val routesBuilder = new RoutesBuilder[F](services.auth)
    List(
      LoginRoutes[F](services.auth, routesBuilder),
      RegisterRoutes[F](services.users, routesBuilder),
      LogoutRoutes[F](services.auth, routesBuilder),
      AdRoutes[F](services.ads, routesBuilder),
      AdChatRoutes[F](services.chats, routesBuilder),
      AdImageRoutes[F](services.images, routesBuilder),
      AdMsgRoutes[F](services.messages, routesBuilder),
      AdTagRoutes[F](services.tags, routesBuilder),
      UserRoutes[F](services.users, routesBuilder),
      UserAdRoutes[F](services.userAds, routesBuilder),
      TagRoutes[F](services.tags, routesBuilder),
      FeedRoutes[F](services.feed, routesBuilder)
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
      val l = Logger[F]
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
