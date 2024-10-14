package maweituo
package tests
package it
package services

import maweituo.domain.all.*
import maweituo.logic.interp.all.*
import maweituo.postgres.repos.all.*
import maweituo.tests.properties.services.MessageServiceProperties
import maweituo.tests.resources.*
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.*

import doobie.util.transactor.Transactor
import weaver.GlobalRead

class MessageServiceITSuite(global: GlobalRead) extends ResourceSuite with MessageServiceProperties:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] = global.postgres

  private def makeTestServices(xa: Transactor[IO])(using LoggerFactory[IO]) =
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
      itTest(name) { postgres =>
        exp.tupled(makeTestServices(postgres))
      }
  }
