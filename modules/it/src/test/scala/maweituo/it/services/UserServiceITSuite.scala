package maweituo.it.services

import cats.effect.IO
import cats.effect.kernel.Resource

import maweituo.domain.services.IAMService
import maweituo.interp.users.UserServiceInterp
import maweituo.postgres.repos.users.PostgresUserRepo
import maweituo.tests.properties.services.UserServiceProperties
import maweituo.tests.resources.*
import maweituo.tests.services.makeIAMService
import maweituo.tests.{ResourceSuite, WeaverLogAdapter}

import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import weaver.GlobalRead

class UserServiceITSuite(global: GlobalRead) extends ResourceSuite with UserServiceProperties:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] = global.postgres

  private def makeTestUsers(xa: Transactor[IO])(using Logger[IO]) =
    given IAMService[IO] = makeIAMService
    val repo             = PostgresUserRepo.make[IO](xa)
    UserServiceInterp.make(repo)

  properties.foreach {
    case Property(name, fn) =>
      itTest(name) { (postgres, log) =>
        given Logger[IO] = WeaverLogAdapter(log)
        fn(makeTestUsers(postgres))
      }
  }
