package maweituo.tests

import cats.effect.IO
import org.typelevel.log4cats.Logger
import weaver.*

abstract class IOLoggedTest extends SimpleIOSuite:
  def ioLoggedTest(name: TestName)(run: Logger[IO] => IO[Expectations]): Unit =
    loggedTest(name)(run.compose[Log[IO]](new WeaverLogAdapter[IO](_)))
