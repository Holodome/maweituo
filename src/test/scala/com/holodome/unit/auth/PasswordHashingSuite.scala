package com.holodome.unit.auth

import cats.effect.IO
import com.holodome.auth.PasswordHashing.{genSalt, hashSaltPassword}
import com.holodome.utils.generators.passwordGen
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers
import cats.syntax.all._

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
