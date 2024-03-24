package com.holodome.storage

import cats.data.OptionT
import cats.effect.{IO, Resource}
import cats.syntax.all._
import com.holodome.auth.{JwtExpire, JwtTokens}
import com.holodome.config.types.{JwtAccessSecret, JwtTokenExpiration}
import com.holodome.domain.users._
import dev.profunktor.auth.jwt.{JwtAuth, JwtToken}
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import com.holodome.generators._
import com.holodome.repositories.redis.{RedisAuthedUserRepository, RedisJwtRepository}
import com.holodome.services.{AuthService, UserService}
import org.typelevel.log4cats.noop.NoOpLogger
import org.typelevel.log4cats.Logger
import pdi.jwt.{JwtAlgorithm, JwtClaim}
import com.holodome.suite.ResourceSuite
import dev.profunktor.redis4cats.effect.Log.NoOp.instance

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.tools.nsc.tasty.SafeEq

class RedisSuite extends ResourceSuite {

  implicit val logger: Logger[IO] = NoOpLogger[IO]

  override type Res = RedisCommands[IO, String, String]

  override def sharedResource: Resource[IO, Res] =
    Redis[IO]
      .utf8("redis://localhost")
      .beforeAll(_.flushAll)

  val jwtSecret   = JwtAccessSecret("test")
  val tokenExp    = JwtTokenExpiration(30.seconds)
  val jwtClaim    = JwtClaim("test")
  val userJwtAuth = UserJwtAuth(JwtAuth.hmac("bar", JwtAlgorithm.HS256))

  test("Authentication") { redis =>
    val gen = for {
      un1 <- usernameGen
      un2 <- usernameGen
      pw  <- passwordGen
    } yield (un1, un2, pw)
    forall(gen) { case (un1, un2, pw) =>
      val jwtRepo     = RedisJwtRepository.make[F](redis, tokenExp)
      val authedRepo  = RedisAuthedUserRepository.make[F](redis, tokenExp)
      val userService = new TestUserService(un2)
      for {
        t <- JwtExpire.make[IO].map(JwtTokens.make[IO](_, jwtSecret, tokenExp))
        a = AuthService.make[IO](userService, jwtRepo, authedRepo, t)
        x <- a.authed(JwtToken("invalid")).value
        y <- a.login(un1, pw).attempt // NoUserFound
      } yield expect.all(
        x.isEmpty,
        y == Left(NoUserFound(un1))
      )
    }
  }
}

protected class TestUserService(un: Username) extends UserService[IO] {

  override def find(name: Username): IO[User] =
    (name === un)
      .guard[Option]
      .as(
        IO {
          User(UserId(UUID.randomUUID), un, Email(""), HashedSaltedPassword(""), PasswordSalt(""))
        }
      )
      .getOrElse(NoUserFound(name).raiseError[IO, User])

  override def register(body: RegisterRequest): IO[Unit] = IO.pure { () }
}
