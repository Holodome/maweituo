package maweituo.it.services

import cats.effect.IO
import cats.effect.kernel.Resource

import maweituo.domain.services.{IAMService, TelemetryService}
import maweituo.logic.interp.ads.{AdServiceInterp, ChatServiceInterp}
import maweituo.logic.interp.users.UserServiceInterp
import maweituo.postgres.repos.ads.{PostgresAdRepo, PostgresChatRepo}
import maweituo.postgres.repos.users.PostgresUserRepo
import maweituo.tests.properties.services.ChatServiceProperties
import maweituo.tests.resources.*
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.*
import maweituo.tests.{ResourceSuite, WeaverLogAdapter}

import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import weaver.GlobalRead

class ChatServiceITSuite(global: GlobalRead) extends ResourceSuite with ChatServiceProperties:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] = global.postgres

  private def makeTestServices(xa: Transactor[IO])(using Logger[IO]) =
    given TelemetryService[IO] = new TelemetryServiceStub[IO]
    val chatRepo               = PostgresChatRepo.make(xa)
    val userRepo               = PostgresUserRepo.make(xa)
    val adRepo                 = PostgresAdRepo.make(xa)
    given IAMService[IO]       = makeIAMService(adRepo, chatRepo)
    val users                  = UserServiceInterp.make(userRepo)
    val ads                    = AdServiceInterp.make(adRepo)
    val chats                  = ChatServiceInterp.make(chatRepo, adRepo)
    (users, ads, chats)

  properties.foreach {
    case Property(name, exp) =>
      itTest(name) { (postgres, log) =>
        given Logger[IO] = WeaverLogAdapter(log)
        exp.tupled(makeTestServices(postgres))
      }
  }
