package maweituo.fa2

import cats.effect.IO
import io.cucumber.scala.{EN, ScalaDsl, Scenario}
import org.junit.Assert.*
import org.http4s.ember.client.EmberClientBuilder
import cats.effect.unsafe.implicits.*

class LoginInterp extends ScalaDsl with EN:

  val client = EmberClientBuilder
    .default[IO]
    .withHttp2
    .build
    .map(AppClient(_))

  val password = sys.env("TEST_PASSWORD")
  val email    = sys.env("TEST_EMAIL")

  var resp = ""

  When("""User send {string} request to \/login""") { (string: String) =>
    println("here")
    resp = client.use { _.login(LoginDTO(email, password)) }.unsafeRunSync()
    println("here1")
  }
  Then("""the response on \/login code should be {int}""") { (int1: Int) =>
  }
  Then("""the response on \/login should match json:""") { (docString: String) =>
    assertEquals(docString, resp)
  }
  Then("""user send {string} request to \/login_verify""") { (string: String) =>
    resp = client.use { _.verifyLogin(VerifyLoginDTO(email, "123")) }.unsafeRunSync()
  }
  Then("""the response on \/login_verify code should be {int}""") { (int1: Int) =>
  }
  Then("""the response on \/login_verify should match json:""") { (docString: String) =>
    assertEquals(docString, resp)
  }

  When("""User send {string} request to \/reset_password""") { (string: String) =>
    resp = client.use { _.password(ResetPasswordDTO(email, password)) }.unsafeRunSync()
  }
  Then("""the response on \/reset_password code should be {int}""") { (int1: Int) =>
  }
  Then("""the response on \/reset_password should match json:""") { (docString: String) =>
    assertEquals(docString, resp)
  }
  Then("""user send {string} request to \/verify_reset_password""") { (string: String) =>
    resp = client.use { _.verifyPassword(VerifyResetPasswordDTO(email, "123", password)) }.unsafeRunSync()
  }
  Then("""the response on \/verify_reset_password code should be {int}""") { (int1: Int) =>
  }
  Then("""the response on \/verify_reset_password should match json:""") { (docString: String) =>
    assertEquals(docString, resp)
  }
