package maweituo
package tests
package auth

import maweituo.logic.auth.PasswordHashing.{ genSalt, hashSaltPassword }

object PasswordHashingSuite extends SimpleIOSuite with Checkers:

  test("basic usage") {
    forall(passwordGen) { password =>
      for
        salt <- genSalt[IO]
        hashedSalted = hashSaltPassword(password, salt)
      yield expect.same(hashedSalted, hashSaltPassword(password, salt))
    }
  }
