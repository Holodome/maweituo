package maweituo.tests.misc

import maweituo.logic.errors.DomainError
import maweituo.domain.users.Username

import io.circe.testing.ArbitraryInstances
import weaver.*
import weaver.discipline.*

object DomainErrorsExceptions extends FunSuite with Discipline with ArbitraryInstances:
  test("check message is meaningful") {
    val error = DomainError.NoUserWithName(Username(""))
    expect.same("NoUserWithName(username = )", error.getMessage())
  }