package maweituo.tests.auth

import cats.effect.IO

import maweituo.logic.auth.PasswordHashing.{ genSalt, hashSaltPassword }
import maweituo.tests.generators.passwordGen

import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object PasswordHashingSuite extends SimpleIOSuite with Checkers:

  test("basic usage") {
    forall(passwordGen) { password =>
      for
        salt <- genSalt[IO]
        hashedSalted = hashSaltPassword(password, salt)
      yield expect.same(hashedSalted, hashSaltPassword(password, salt))
    }
  }
