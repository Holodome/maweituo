package maweituo.tests.properties.services

import cats.effect.IO
import cats.syntax.all.*

import maweituo.auth.JwtTokens
import maweituo.domain.errors.*
import maweituo.domain.users.services.{AuthService, UserService}
import maweituo.domain.users.{AuthedUser, UserId}
import maweituo.tests.generators.*
import maweituo.tests.utils.given

import dev.profunktor.auth.jwt.JwtToken
import org.scalacheck.Gen
import weaver.scalacheck.Checkers
import weaver.{Expectations, MutableIOSuite}

trait AuthServiceProperties:
  this: MutableIOSuite & Checkers =>

  protected final case class Property(
      name: String,
      exp: (JwtTokens[IO] => (UserService[IO], AuthService[IO])) => IO[Expectations]
  )

  private def defaultJwt               = new TestJwtTokens(JwtToken("test"))
  private def makeJwt(token: JwtToken) = new TestJwtTokens(token)

  private val jwtGen: Gen[JwtToken] = nesGen(JwtToken.apply)

  protected val properties = List(
    Property(
      "login on invalid user fails",
      create =>
        val (users, auth) = create(defaultJwt)
        val gen =
          for
            name     <- usernameGen
            password <- passwordGen
          yield name -> password
        forall(gen) { case (name, password) =>
          for
            x <- auth.login(name, password).attempt
          yield expect.same(Left(NoUserWithName(name)), x)
        }
    ),
    Property(
      "unauthenticated user",
      create =>
        forall(jwtGen) { token =>
          val (_, auth) = create(makeJwt(token))
          for
            x <- auth.authed(token).value
          yield expect(x.isEmpty)
        }
    ),
    Property(
      "login works",
      create =>
        val gen =
          for
            reg <- registerGen
            jwt <- jwtGen
          yield reg -> jwt
        forall(gen) { (reg, jwt) =>
          val (users, auth) = create(makeJwt(jwt))
          for
            id     <- users.create(reg)
            (t, _) <- auth.login(reg.name, reg.password)
            x      <- auth.authed(t).value
          yield expect.same(t, jwt) and expect.same(Some(AuthedUser(id)), x)
        }
    ),
    Property(
      "login with invalid password",
      create =>
        val gen =
          for
            reg  <- registerGen
            pass <- passwordGen
          yield reg -> pass
        val (users, auth) = create(defaultJwt)
        forall(gen) { (reg, pass) =>
          for
            _ <- users.create(reg)
            x <- auth.login(reg.name, pass).attempt
          yield expect.same(Left(InvalidPassword(reg.name)), x)
        }
    ),
    Property(
      "logout works",
      create =>
        val gen =
          for
            reg <- registerGen
            jwt <- jwtGen
          yield reg -> jwt
        forall(gen) { (reg, jwt) =>
          val (users, auth) = create(makeJwt(jwt))
          for
            id     <- users.create(reg)
            (t, _) <- auth.login(reg.name, reg.password)
            _      <- auth.logout(id, t)
            x      <- auth.authed(t).value
          yield expect(x.isEmpty)
        }
    )
  )

protected final class TestJwtTokens(tok: JwtToken) extends JwtTokens[IO]:
  def create(userId: UserId): IO[JwtToken] = tok.pure[IO]
