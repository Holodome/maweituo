package com.holodome.tests

import java.nio.charset.StandardCharsets

import scala.util.control.NoStackTrace

import cats.Applicative
import cats.effect.IO
import cats.syntax.all.*
import io.circe.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.typelevel.log4cats.Logger
import weaver.scalacheck.Checkers
import weaver.{ Expectations, SimpleIOSuite, SourceLocation }

trait HttpSuite extends SimpleIOSuite with Checkers:

  case object DummyError extends NoStackTrace

  def expectHttpBodyAndStatus[A: Encoder](routes: HttpRoutes[IO], req: Request[IO])(
      expectedBody: A,
      expectedStatus: org.http4s.Status
  )(using loc: SourceLocation): IO[Expectations] =
    routes.run(req).value.flatMap {
      case Some(resp) =>
        resp.asJson.map { json =>
          expect.same(resp.status, expectedStatus)(loc = loc) |+| expect
            .same(json.dropNullValues, expectedBody.asJson.dropNullValues)(loc = loc)
        }
      case None => failure("route not found")(loc).pure[IO]
    }

  def expectHttpStatus(routes: HttpRoutes[IO], req: Request[IO])(
      expectedStatus: org.http4s.Status
  )(using loc: SourceLocation): IO[Expectations] =
    routes.run(req).value.map {
      case Some(resp) => expect.same(resp.status, expectedStatus)(loc = loc)
      case None       => failure("route not found")(loc)
    }

  def expectHttpFailure(routes: HttpRoutes[IO], req: Request[IO])(using loc: SourceLocation): IO[Expectations] =
    routes.run(req).value.attempt.map {
      case Left(_)  => success
      case Right(_) => failure("expected a failure")(loc)
    }

  def expectHttpStatusLogged(routes: HttpRoutes[IO], req: Request[IO])(
      expectedStatus: org.http4s.Status
  )(using loc: SourceLocation, l: Logger[IO]): IO[Expectations] =
    routes
      .run(req)
      .value
      .flatTap {
        case Some(resp) =>
          resp.body.compile.toVector.flatMap { bodyVec =>
            val str = new String(bodyVec.toArray, StandardCharsets.UTF_8)
            l.info(s"$resp body '$str''")
          }
        case None => Applicative[F].unit
      }
      .map {
        case Some(resp) => expect.same(resp.status, expectedStatus)(loc = loc)
        case None       => failure("route not found")(loc)
      }
