package com.holodome.tests.auth

import cats.effect.IO
import cats.syntax.all._
import com.holodome.auth.PasswordHashing.{genSalt, hashSaltPassword}
import com.holodome.tests.generators.passwordGen
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object PasswordHashingSuite extends SimpleIOSuite with Checkers {

  test("basic usage") {
    forall(passwordGen) { password =>
      for {
        salt <- genSalt[IO]
        hashedSalted = hashSaltPassword(password, salt)
      } yield expect.all(hashedSalted === hashSaltPassword(password, salt))
    }
  }
}
