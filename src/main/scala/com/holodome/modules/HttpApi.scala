package com.holodome.modules

import cats.effect.Async
import cats.implicits.toSemigroupKOps
import com.holodome.domain.users.{AuthedUser, UserJwtAuth}
import com.holodome.http.routes.{AdvertisementRoutes, LoginRoutes, LogoutRoutes, MessageRoutes}
import com.holodome.http.routes
import dev.profunktor.auth.JwtAuthMiddleware
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.server.middleware.{AutoSlash, CORS, RequestLogger, ResponseLogger, Timeout}

import scala.concurrent.duration.DurationInt

object HttpApi {
  def make[F[_]: Async](services: Services[F], userJwtAuth: UserJwtAuth): HttpApi[F] =
    new HttpApi[F](services, userJwtAuth) {}
}

sealed abstract class HttpApi[F[_]: Async](services: Services[F], userJwtAuth: UserJwtAuth) {
  private val usersMiddleware =
    JwtAuthMiddleware[F, AuthedUser](userJwtAuth.value, t => _ => services.auth.authed(t).value)

  private val loginRoutes  = LoginRoutes[F](services.auth).routes
  private val logoutRoutes = LogoutRoutes[F](services.auth).routes(usersMiddleware)
  private val advertisementRoutes =
    AdvertisementRoutes[F](services.ads, services.chats).routes(usersMiddleware)
  private val msgRoutes = MessageRoutes[F](services.messages).routes(usersMiddleware)

  private val routes: HttpRoutes[F] =
    loginRoutes <+> logoutRoutes <+> advertisementRoutes <+> msgRoutes
  private val middleware: HttpRoutes[F] => HttpRoutes[F] = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    } andThen { http: HttpRoutes[F] =>
      CORS(http)
    } andThen { http: HttpRoutes[F] =>
      Timeout(60.seconds)(http)
    }
  }

  private val loggers: HttpApp[F] => HttpApp[F] = {
    { http: HttpApp[F] =>
      RequestLogger.httpApp(logHeaders = true, logBody = true)(http)
    } andThen { http: HttpApp[F] =>
      ResponseLogger.httpApp(logHeaders = true, logBody = true)(http)
    }
  }

  val httpApp: HttpApp[F] = loggers(middleware(routes).orNotFound)
}
