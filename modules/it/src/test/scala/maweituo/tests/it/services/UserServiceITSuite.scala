package maweituo
package tests
package it
package services

import maweituo.domain.all.*
import maweituo.logic.interp.all.*
import maweituo.postgres.repos.all.*
import maweituo.tests.properties.services.UserServiceProperties
import maweituo.tests.resources.*
import maweituo.tests.services.makeIAMService

import doobie.util.transactor.Transactor
import weaver.GlobalRead

class UserServiceITSuite(global: GlobalRead) extends ResourceSuite with UserServiceProperties:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] = global.postgres

  private def makeTestUsers(xa: Transactor[IO])(using LoggerFactory[IO]) =
    given IAMService[IO] = makeIAMService
    val repo             = PostgresUserRepo.make[IO](xa)
    UserServiceInterp.make(repo)

  properties.foreach {
    case Property(name, fn) =>
      itTest(name) { (postgres, log) =>
        given LoggerFactory[IO] = WeaverLogAdapterFactory[IO](log)
        fn(makeTestUsers(postgres))
      }
  }
