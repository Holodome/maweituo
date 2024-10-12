package maweituo
package modules

import scala.concurrent.duration.DurationInt

import cats.Parallel
import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.*

import maweituo.http.*
import maweituo.http.routes.all.*

import org.http4s.implicits.*
import org.http4s.server.middleware.*
import org.http4s.{HttpApp, HttpRoutes}
import org.typelevel.log4cats.Logger
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object HttpApi:
  def make[F[_]: Async: Logger: Parallel](services: Services[F]): HttpApi[F] =
    new HttpApi[F](services)

sealed class HttpApi[F[_]: Async: Logger: Parallel](
    services: Services[F]
):

  private val endpoints =
    val routesBuilder = new RoutesBuilder[F](services.auth)
    List(
      LoginRoutes[F](services.auth, routesBuilder),
      LogoutRoutes[F](services.auth, routesBuilder),
      RegisterRoutes[F](services.users, routesBuilder),
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

  private val swaggerEndpoints = SwaggerInterpreter().fromServerEndpoints[F](endpoints, "maweituo", "0.1")

  private val routes =
    Http4sServerInterpreter[F]().toRoutes(endpoints) <+> Http4sServerInterpreter[F]().toRoutes(swaggerEndpoints)

  private val middleware: HttpRoutes[F] => HttpRoutes[F] = { (http: HttpRoutes[F]) =>
    AutoSlash(http)
  } andThen { (http: HttpRoutes[F]) =>
    CORS.policy.withAllowOriginAll
      .withAllowCredentials(false)
      .apply(http)
  } andThen { (http: HttpRoutes[F]) =>
    Timeout(60.seconds)(http)
  }

  private val loggers: HttpApp[F] => HttpApp[F] = { (http: HttpApp[F]) =>
    RequestLogger.httpApp(logHeaders = true, logBody = true)(http)
  } andThen { (http: HttpApp[F]) =>
    ResponseLogger.httpApp(logHeaders = true, logBody = true)(http)
  }

  private def httpDomainErrorHandler(t: Throwable, msg: => String): OptionT[F, Unit] =
    OptionT.liftF(
      org.typelevel.log4cats.Logger[F].error(t)(msg)
    )

  def withErrorLogging(routes: HttpRoutes[F]) = ErrorHandling.Recover.total(
    ErrorAction.log(
      routes,
      messageFailureLogAction = httpDomainErrorHandler,
      serviceErrorLogAction = httpDomainErrorHandler
    )
  )

  val httpApp: HttpApp[F] = loggers(middleware(withErrorLogging(routes)).orNotFound)
