package maweituo
package tests
package http

import cats.data.Kleisli

import maweituo.domain.all.*
import maweituo.http.*

import org.http4s.*
import org.typelevel.log4cats.Logger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

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
