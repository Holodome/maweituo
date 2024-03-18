package com.holodome.services

import cats.Applicative
import cats.data.{EitherT, Reader}
import cats.effect.IO
import com.holodome.models.auth.Login
import com.holodome.models.{LoginError, User}
import com.holodome.repositories.UserRepository

import java.security.MessageDigest

trait UserService[F[_]] {
  def login(
      body: Login.Request
  ): Reader[UserRepository[F], EitherT[F, LoginError, Unit]]
}

object UserService extends UserService[IO] {
  override def login(
      body: Login.Request
  ): Reader[UserRepository[IO], EitherT[IO, LoginError, Unit]] =
    Reader { repo =>
      repo
        .findByEmail(body.email)
        .toRight(LoginError("User not found"))
        .flatMap {
          case u if passwordsMatch(u, body.password) =>
            EitherT.right(IO.pure(()))
          case _ => EitherT.leftT(LoginError("Invalid password"))
        }
    }

  private def passwordHash(password: String, salt: String): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest((password + salt).getBytes("UTF-8"))
      .map("%02x".format(_))
      .mkString

  private def passwordsMatch(user: User, password: String): Boolean =
    passwordHash(password, user.salt) == user.hashedPassword
}
