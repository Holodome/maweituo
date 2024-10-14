package maweituo
package tests
package it
package services

import maweituo.domain.all.*
import maweituo.logic.interp.all.*
import maweituo.tests.properties.services.ChatServiceProperties
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.*

import doobie.util.transactor.Transactor
import weaver.GlobalRead

class ChatServiceITSuite(global: GlobalRead) extends PostgresITSuite(global) with ChatServiceProperties:

  private def makeTestServices(xa: Transactor[IO])(using LoggerFactory[IO]) =
    given TelemetryService[IO] = TelemetryServiceStub[IO]
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
      pgTest(name) { postgres =>
        exp.tupled(makeTestServices(postgres))
      }
  }
