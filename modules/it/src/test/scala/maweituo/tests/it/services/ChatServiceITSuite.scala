package maweituo
package tests
package it
package services

import maweituo.domain.all.*
import maweituo.logic.interp.all.*
import maweituo.postgres.repos.all.*
import maweituo.tests.properties.services.ChatServiceProperties
import maweituo.tests.resources.*
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.*

import doobie.util.transactor.Transactor
import weaver.GlobalRead

class ChatServiceITSuite(global: GlobalRead) extends ResourceSuite with ChatServiceProperties:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] = global.postgres

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
      itTest(name) { (postgres, log) =>
        given LoggerFactory[IO] = WeaverLogAdapterFactory[IO](log)
        exp.tupled(makeTestServices(postgres))
      }
  }
