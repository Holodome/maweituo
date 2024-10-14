package maweituo
package tests
package it
package services

import maweituo.domain.all.*
import maweituo.logic.interp.all.*
import maweituo.tests.properties.services.UserServiceProperties
import maweituo.tests.services.makeIAMService

import doobie.util.transactor.Transactor
import weaver.GlobalRead

class UserServiceITSuite(global: GlobalRead) extends PostgresITSuite(global) with UserServiceProperties:

  private def makeTestUsers(xa: Transactor[IO])(using LoggerFactory[IO]) =
    given IAMService[IO] = makeIAMService
    val repo             = PostgresUserRepo.make[IO](xa)
    UserServiceInterp.make(repo)

  properties.foreach {
    case Property(name, fn) =>
      pgTest(name) { postgres =>
        fn(makeTestUsers(postgres))
      }
  }
