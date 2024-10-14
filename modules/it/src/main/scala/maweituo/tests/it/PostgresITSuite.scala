package maweituo
package tests
package it

import maweituo.tests.containers.PartiallyAppliedPostgres
import maweituo.tests.resources.postgres

import doobie.util.transactor.Transactor
import org.typelevel.log4cats.LoggerFactory
import weaver.*
import weaver.scalacheck.CheckConfig

abstract class PostgresITSuite(global: GlobalRead) extends ResourceSuite:
  export maweituo.postgres.repos.all.*

  override def checkConfig: CheckConfig = CheckConfig.default.copy(minimumSuccessful = 1, perPropertyParallelism = 1)

  type Res = PartiallyAppliedPostgres

  override def sharedResource: Resource[IO, Res] = global.postgres

  def pgTest(name: String)(fn: LoggerFactory[IO] ?=> Transactor[IO] => IO[Expectations]) =
    itTest(name) { pg0 =>
      pg0().use { postgres =>
        fn(postgres)
      }
    }
