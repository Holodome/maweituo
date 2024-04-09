package com.holodome

import cats.effect.IO
import weaver.{Expectations, Log, SimpleIOSuite, Test, TestName}

abstract class IOLoggedTest extends SimpleIOSuite {
  def ioLoggedTest(name: TestName)(run: WeaverLogAdapter[IO] => IO[Expectations]): Unit =
    registerTest(name)(_ => Test(name.name, (log: Log[IO]) => run(new WeaverLogAdapter[IO](log))))
}
