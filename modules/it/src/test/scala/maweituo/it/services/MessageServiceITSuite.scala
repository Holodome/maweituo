package maweituo.it.services

import cats.MonadThrow
import cats.effect.IO
import cats.effect.kernel.Resource

import maweituo.domain.services.{IAMService, TelemetryService}
import maweituo.interp.*
import maweituo.interp.ads.{AdServiceInterp, ChatServiceInterp, MessageServiceInterp}
import maweituo.interp.users.UserServiceInterp
import maweituo.postgres.repos.ads.{PostgresAdRepo, PostgresChatRepo, PostgresMessageRepo}
import maweituo.postgres.repos.users.PostgresUserRepo
import maweituo.tests.properties.services.MessageServiceProperties
import maweituo.tests.resources.*
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.*
import maweituo.tests.{ResourceSuite, WeaverLogAdapter}

import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import weaver.GlobalRead

class MessageServiceITSuite(global: GlobalRead) extends ResourceSuite with MessageServiceProperties:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] = global.postgres

  private def makeTestServices(xa: Transactor[IO])(using Logger[IO]) =
    given TelemetryService[IO] = new TelemetryServiceStub[IO]
    val chatRepo               = PostgresChatRepo.make(xa)
    val userRepo               = PostgresUserRepo.make(xa)
    val adRepo                 = PostgresAdRepo.make(xa)
    val msgRepo                = PostgresMessageRepo.make(xa)
    given iam: IAMService[IO]  = makeIAMService(adRepo, chatRepo)
    val users                  = UserServiceInterp.make(userRepo)
    val ads                    = AdServiceInterp.make(adRepo)
    val chats                  = ChatServiceInterp.make(chatRepo, adRepo)
    val msgs                   = MessageServiceInterp.make(msgRepo)(using MonadThrow[IO], timeSource, iam)
    (users, ads, chats, msgs)

  properties.foreach {
    case Property(name, exp) =>
      itTest(name) { (postgres, log) =>
        given Logger[IO] = new WeaverLogAdapter[IO](log)
        exp.tupled(makeTestServices(postgres))
      }
  }
