package com.holodome.tests.auth

import com.holodome.auth.PasswordHashing.{ genSalt, hashSaltPassword }
import com.holodome.tests.generators.passwordGen

import cats.effect.IO
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
