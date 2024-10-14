package maweituo
package tests

import cats.effect.std.Env

import weaver.{BaseIOSuite, IOSuite, TestName}

sealed trait Helpers:
  this: BaseIOSuite =>

  val itTestIgnore =
    Env[IO].get("CI")
      .flatMap {
        case Some("1") =>
          Env[IO].get("UNIT_SUCCESS").flatMap {
            case Some("1") => IO.unit
            case _         => ignore("Unit tests failed")
          }
        case _ => IO.unit
      }

  val e2eTestIgnore =
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

abstract class MaweituoSimpleSuite extends SimpleIOSuite with Checkers with Helpers:

  def unitTest(name: TestName)(run: LoggerFactory[IO] ?=> IO[Expectations]) =
    loggedTest(name) { log =>
      given LoggerFactory[F] = WeaverLogAdapterFactory(log)
      run
    }

  def itTest(name: TestName)(run: LoggerFactory[IO] ?=> IO[Expectations]) =
    test(name) { (res, log) =>
      given LoggerFactory[F] = WeaverLogAdapterFactory(log)
      itTestIgnore *> run
    }

  def e2eTest(name: TestName)(run: LoggerFactory[IO] ?=> IO[Expectations]) =
    test(name) { (res, log) =>
      given LoggerFactory[F] = WeaverLogAdapterFactory(log)
      e2eTestIgnore *> run
    }

abstract class MaweituoSuite extends IOSuite with Checkers with Helpers:

  def unitTest(name: TestName)(run: LoggerFactory[IO] ?=> Res => IO[Expectations]) =
    test(name) { (res, log) =>
      given LoggerFactory[F] = WeaverLogAdapterFactory(log)
      run(res)
    }

  def itTest(name: TestName)(run: LoggerFactory[IO] ?=> Res => IO[Expectations]) =
    test(name) { (res, log) =>
      given LoggerFactory[F] = WeaverLogAdapterFactory(log)
      itTestIgnore *> run(res)
    }

  def e2eTest(name: TestName)(run: LoggerFactory[IO] ?=> Res => IO[Expectations]) =
    test(name) { (res, log) =>
      given LoggerFactory[F] = WeaverLogAdapterFactory(log)
      e2eTestIgnore *> run(res)
    }
