package com.holodome.modules

import scala.concurrent.duration.DurationInt

import com.holodome.domain.users.AuthedUser
import com.holodome.domain.users.UserJwtAuth
import com.holodome.http.routes.*
import com.holodome.http.routes.ads.*
import com.holodome.http.{ *, given }

import cats.Parallel
import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.*
import dev.profunktor.auth.JwtAuthMiddleware
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.implicits.*
import org.http4s.server.middleware.*
import org.typelevel.log4cats.Logger
import pdi.jwt.JwtClaim

object HttpApi:
  def make[F[_]: Async: Logger: Parallel](services: Services[F], userJwtAuth: UserJwtAuth): HttpApi[F] =
    new HttpApi[F](services, userJwtAuth)

sealed class HttpApi[F[_]: Async: Logger: Parallel](
    services: Services[F],
    userJwtAuth: UserJwtAuth
):

  private val usersMiddleware =
    JwtAuthMiddleware[F, AuthedUser](userJwtAuth.value, t => (c: JwtClaim) => services.auth.authed(t).value)

  private val loginRoutes    = LoginRoutes[F](services.auth).routes
  private val logoutRoutes   = LogoutRoutes[F](services.auth).routes(usersMiddleware)
  private val registerRoutes = RegisterRoutes[F](services.users).routes

  private val adRoutes      = AdRoutes[F](services.ads).routes(usersMiddleware)
  private val adChatRoutes  = AdChatRoutes[F](services.chats).routes(usersMiddleware)
  private val adImageRoutes = AdImageRoutes[F](services.images).routes(usersMiddleware)
  private val adMsgRoutes   = AdMsgRoutes[F](services.messages).routes(usersMiddleware)
  private val adTagRoutes   = AdTagRoutes[F](services.ads).routes(usersMiddleware)

  private val userRoutes = UserRoutes[F](services.users).routes(usersMiddleware)

  private val tagRoutes = TagRoutes[F](services.tags).routes

  private val feedRoutes = FeedRoutes[F](services.feed).routes(usersMiddleware)

  private val routes: HttpRoutes[F] =
    (loginRoutes |+| registerRoutes |+| tagRoutes |+| adRoutes |+| adChatRoutes |+| adImageRoutes |+| adMsgRoutes |+| adTagRoutes |+| userRoutes |+| logoutRoutes |+| feedRoutes).collapse

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
    RequestLogger.httpApp(logHeaders = true, logBody = false)(http)
  } andThen { (http: HttpApp[F]) =>
    ResponseLogger.httpApp(logHeaders = true, logBody = false)(http)
  }

  private def errorHandler(t: Throwable, msg: => String): OptionT[F, Unit] =
    OptionT.liftF(
      org.typelevel.log4cats.Logger[F].error(t)(msg)
    )

  val httpApp: HttpApp[F] = loggers(middleware(routes).orNotFound)
