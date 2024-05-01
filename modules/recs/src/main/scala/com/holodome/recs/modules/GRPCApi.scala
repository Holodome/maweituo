package com.holodome.recs.modules

import cats.data.OptionT
import cats.effect.kernel.Async
import cats.syntax.all._
import com.holodome.effects.GenUUID
import com.holodome.recs.grpc.{RecommendationGRPCServer, TelemetryGRPCServer}
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.server.middleware._
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.DurationInt

object GRPCApi {
  def make[F[_]: GenUUID: Async: Logger](services: Services[F]): GRPCApi[F] =
    new GRPCApi(services) {}
}

sealed class GRPCApi[F[_]: GenUUID: Async: Logger] private (services: Services[F]) {
  private val recs      = RecommendationGRPCServer.make[F](services.recs)
  private val telemetry = TelemetryGRPCServer.make[F](services.telemetry)

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

  private val routes      = recs <+> telemetry
  val httpApp: HttpApp[F] = loggers(middleware(withErrorLogging(routes)).orNotFound)
}
