package com.holodome.tests

import cats.effect.{IO, Resource}
import cats.syntax.all._
import weaver.scalacheck.{CheckConfig, Checkers}
import weaver.{Expectations, IOSuite}

abstract class ResourceSuite extends IOSuite with Checkers {

  override def checkConfig: CheckConfig = CheckConfig.default

  implicit class SharedResOps(res: Resource[IO, Res]) {
    def beforeAll(f: Res => IO[Unit]): Resource[IO, Res] =
      res.evalTap(f)

    def afterAll(f: Res => IO[Unit]): Resource[IO, Res] =
      res.flatTap(x => Resource.make(IO.unit)(_ => f(x)))
  }

  def testBeforeAfterEach(
      before: Res => IO[Unit],
      after: Res => IO[Unit]
  ): String => (Res => IO[Expectations]) => Unit =
    name => fa => test(name)(res => before(res) >> fa(res).guarantee(after(res)))

  def testAfterEach(after: Res => IO[Unit]): String => (Res => IO[Expectations]) => Unit =
    testBeforeAfterEach(_ => IO.unit, after)

  def testBeforeEach(before: Res => IO[Unit]): String => (Res => IO[Expectations]) => Unit =
    testBeforeAfterEach(before, _ => IO.unit)

}
