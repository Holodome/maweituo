package maweituo.modules

import scala.concurrent.duration.DurationInt

import cats.Parallel
import cats.data.OptionT
import cats.effect.Async

import maweituo.domain.users.{AuthedUser, UserJwtAuth}
import maweituo.http.*
import maweituo.http.routes.*
import maweituo.http.routes.ads.*

import dev.profunktor.auth.JwtAuthMiddleware
import org.http4s.implicits.*
import org.http4s.server.middleware.*
import org.http4s.{HttpApp, HttpRoutes}
import org.typelevel.log4cats.Logger
import pdi.jwt.JwtClaim

object HttpApi:
  def make[F[_]: Async: Logger: Parallel](services: Services[F], userJwtAuth: UserJwtAuth): HttpApi[F] =
    new HttpApi[F](services, userJwtAuth)

sealed class HttpApi[F[_]: Async: Logger: Parallel](
    services: Services[F],
    userJwtAuth: UserJwtAuth
):

  private val routeList = List(
    LoginRoutes[F](services.auth),
    LogoutRoutes[F](services.auth),
    RegisterRoutes[F](services.users),
    AdRoutes[F](services.ads),
    AdChatRoutes[F](services.chats),
    AdImageRoutes[F](services.images),
    AdMsgRoutes[F](services.messages),
    AdTagRoutes[F](services.tags),
    UserRoutes[F](services.users, services.userAds),
    TagRoutes[F](services.tags),
    FeedRoutes[F](services.feed)
  )

  private val usersMiddleware =
    JwtAuthMiddleware[F, AuthedUser](userJwtAuth.value, t => (c: JwtClaim) => services.auth.authed(t).value)

  private val routes = buildRoutes(routeList, usersMiddleware)

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

  private def errorHandler(t: Throwable, msg: => String): OptionT[F, Unit] =
    OptionT.liftF(
      org.typelevel.log4cats.Logger[F].error(t)(msg)
    )

  def withErrorLogging(routes: HttpRoutes[F]) = ErrorHandling.Recover.total(
    ErrorAction.log(
      routes,
      messageFailureLogAction = errorHandler,
      serviceErrorLogAction = errorHandler
    )
  )

  val httpApp: HttpApp[F] = loggers(middleware(withErrorLogging(routes)).orNotFound)
