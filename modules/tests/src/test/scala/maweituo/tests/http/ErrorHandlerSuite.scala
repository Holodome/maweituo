package maweituo.tests.http

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.syntax.all.*

import maweituo.domain.errors.DomainError
import maweituo.domain.users.Username
import maweituo.http.dto.ErrorResponseDto
import maweituo.http.errors.httpDomainErrorHandler
import maweituo.tests.{HttpSuite, WeaverLogAdapter}

import org.http4s.*
import org.typelevel.log4cats.Logger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object httpDomainErrorHandlerSuite extends SimpleIOSuite with Checkers with HttpSuite:

  loggedTest("domain errors are converted to http") { log =>
    given Logger[IO] = WeaverLogAdapter(log)
    val handler      = httpDomainErrorHandler[IO]
    val errorRoutes = Kleisli { (req: Request[F]) =>
      OptionT.liftF(
        DomainError.NoUserWithName(Username("test")).raiseError[IO, Response[IO]]
      )
    }
    expectHttpBodyAndStatus(handler(errorRoutes), Request())(
      ErrorResponseDto(List("no user with name test found")),
      Status.BadRequest
    )
  }
