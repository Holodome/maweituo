package maweituo
package tests
package misc

import maweituo.domain.all.*
import maweituo.logic.DomainError

import io.circe.testing.ArbitraryInstances
import weaver.*
import weaver.discipline.*

object DomainErrorsExceptions extends FunSuite with Discipline with ArbitraryInstances:
  test("check message is meaningful") {
    val error = DomainError.NoUserWithName(Username(""))
    expect.same("NoUserWithName(username = )", error.getMessage())
  }