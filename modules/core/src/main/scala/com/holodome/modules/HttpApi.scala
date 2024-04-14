package com.holodome.modules

import cats.syntax.all._
import cats.data.OptionT
import cats.effect.Async
import com.holodome.http._
import com.holodome.domain.errors.ApplicationError
import com.holodome.domain.users.{AuthedUser, UserJwtAuth}
import com.holodome.http.routes._
import com.holodome.http.HttpErrorHandler
import com.holodome.http.routes.ads._
import dev.profunktor.auth.JwtAuthMiddleware
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.middleware._
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.DurationInt

object HttpApi {
  def make[F[_]: Async: Logger](services: Services[F], userJwtAuth: UserJwtAuth)(implicit
      H: HttpErrorHandler[F, ApplicationError]
  ): HttpApi[F] =
    new HttpApi[F](services, userJwtAuth)
}

sealed class HttpApi[F[_]: Async: Logger](services: Services[F], userJwtAuth: UserJwtAuth)(implicit
    H: HttpErrorHandler[F, ApplicationError]
) {
  private val usersMiddleware =
    JwtAuthMiddleware[F, AuthedUser](userJwtAuth.value, t => _ => services.auth.authed(t).value)

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

  private val routes: HttpRoutes[F] =
    (loginRoutes |+| registerRoutes |+| tagRoutes |+| adRoutes |+| adChatRoutes |+| adImageRoutes |+| adMsgRoutes |+| adTagRoutes |+| userRoutes |+| logoutRoutes).collapse

  private val middleware: HttpRoutes[F] => HttpRoutes[F] = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    } andThen { http: HttpRoutes[F] =>
      CORS.policy.withAllowOriginAll
        .withAllowCredentials(false)
        .apply(http)
    } andThen { http: HttpRoutes[F] =>
      Timeout(60.seconds)(http)
    }
  }

  private val loggers: HttpApp[F] => HttpApp[F] = {
    { http: HttpApp[F] =>
      RequestLogger.httpApp(logHeaders = true, logBody = false)(http)
    } andThen { http: HttpApp[F] =>
      ResponseLogger.httpApp(logHeaders = true, logBody = false)(http)
    }
  }

  private def errorHandler(t: Throwable, msg: => String): OptionT[F, Unit] =
    OptionT.liftF(
      org.typelevel.log4cats.Logger[F].error(t)(msg)
    )

  private def withErrorLogging(routes: HttpRoutes[F]) = ErrorHandling.Recover.total(
    ErrorAction.log(
      routes,
      messageFailureLogAction = errorHandler,
      serviceErrorLogAction = errorHandler
    )
  )

  val httpApp: HttpApp[F] = loggers(middleware(withErrorLogging(routes)).orNotFound)
}
