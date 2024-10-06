package maweituo.tests

import cats.effect.*
import cats.syntax.all.*

import weaver.IOSuite
import weaver.scalacheck.{CheckConfig, Checkers}
import weaver.TestName
import weaver.Expectations
import weaver.Log
import weaver.Test
import cats.effect.std.Env
import cats.Applicative

abstract class ResourceSuite extends IOSuite with Checkers:
  // For it:tests, one is enough
  override def checkConfig: CheckConfig = CheckConfig.default.copy(minimumSuccessful = 1)

  extension (res: Resource[IO, Res])
    def beforeAll(f: Res => IO[Unit]): Resource[IO, Res] =
      res.evalTap(f)

    def afterAll(f: Res => IO[Unit]): Resource[IO, Res] =
      res.flatTap(x => Resource.make(IO.unit)(_ => f(x)))

  private val itTestIgnore =
    Env[IO].get("CI")
      .flatMap {
        case Some("1") =>
          Env[IO].get("UNIT_SUCCESS").flatMap {
            case Some("1") => IO.unit
            case _         => ignore("Unit tests failed")
          }
        case _ => IO.unit
      }

  private val e2eTestIgnore =
    Env[IO].get("CI")
      .flatMap {
        case Some("1") =>
          (Env[IO].get("UNIT_SUCCESS"), Env[IO].get("INTEGRATION_SUCCESS")).parFlatMapN {
            case (Some("1"), Some("1")) => IO.unit
            case (Some("1"), _)         => ignore("Integration tests failed")
            case _                      => ignore("Unit tests failed")
          }
        case _ => IO.unit
      }

  def itTest(name: TestName)(run: Res => F[Expectations]) =
    test(name) { res => itTestIgnore *> run(res) }

  def itTest(name: TestName)(run: (Res, Log[F]) => F[Expectations]) =
    test(name) { (res, log) => itTestIgnore *> run(res, log) }

  def e2eTest(name: TestName)(run: Res => F[Expectations]) =
    test(name) { res => e2eTestIgnore *> run(res) }

  def e2eTest(name: TestName)(run: (Res, Log[F]) => F[Expectations]) =
    test(name) { (res, log) => e2eTestIgnore *> run(res, log) }
