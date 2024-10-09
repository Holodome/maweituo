package maweituo.tests.http

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.syntax.all.*

import maweituo.logic.errors.DomainError
import maweituo.domain.users.Username
import maweituo.http.dto.ErrorResponseDto
import maweituo.http.errors.HttpDomainErrorHandler
import maweituo.tests.{HttpSuite, WeaverLogAdapter}

import org.http4s.*
import org.typelevel.log4cats.Logger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers
import cats.data.NonEmptyList

object httpDomainErrorHandlerSuite extends SimpleIOSuite with Checkers with HttpSuite:

  loggedTest("domain errors are converted to http") { log =>
    given Logger[IO] = WeaverLogAdapter(log)
    val errorRoutes = Kleisli { (req: Request[F]) =>
      OptionT.liftF(
        DomainError.NoUserWithName(Username("test")).raiseError[IO, Response[IO]]
      )
    }
    expectHttpBodyAndStatus(HttpDomainErrorHandler(errorRoutes), Request())(
      ErrorResponseDto(NonEmptyList.one("no user with name test found")),
      Status.BadRequest
    )
  }
