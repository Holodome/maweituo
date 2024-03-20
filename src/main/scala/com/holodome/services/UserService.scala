package com.holodome.services

import cats.data.{EitherT, OptionT, Reader}
import cats.effect.IO
import com.holodome.domain.auth._
import com.holodome.domain.User
import com.holodome.repositories.UserRepository
import cats._
import cats.syntax.all._
import com.holodome.effects.GenUUID

import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

trait UserService[F[_]] {
  def login(
      body: LoginRequest
  ): F[UUID]

  def register(
      body: RegisterRequest
  ): F[UUID]
}

object UserService {
  private class Impl[F[_]: MonadThrow: GenUUID](repo: UserRepository[F])
      extends UserService[F] {
    override def login(
        body: LoginRequest
    ): F[UUID] =
      repo
        .findByName(body.name)
        .getOrElseF(NoUserFound(body.name).raiseError[F, User])
        .flatMap {
          case u if passwordsMatch(u, body.password) => u.id.pure[F]
          case _                                     => InvalidPassword(body.name).raiseError[F, UUID]
        }

    override def register(
        body: RegisterRequest
    ): F[UUID] = {
      val user = for {
        _ <- repo
          .findByEmail(body.email)
          .getOrElse(UserEmailInUse(body.email).raiseError[F, Unit])
        _ <- repo
          .findByName(body.name)
          .getOrElse(UserNameInUse(body.name).raiseError[F, Unit])
        salt <- GenUUID[F].make
        user = User.CreateUser(
          body.name,
          body.email,
          hashString(body.password),
          salt.toString,
          Instant.now
        )
      } yield user
      user.flatMap(u => repo.create(u))
    }
  }
  def make[F[_]: MonadThrow: GenUUID](repo: UserRepository[F]): UserService[F] =
    new Impl(repo)

  private def hashString(str: String): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(str.getBytes("UTF-8"))
      .map("%02x".format(_))
      .mkString

  private def passwordsMatch(user: User, password: String): Boolean =
    hashString(password + user.salt) == user.hashedPassword

  private def genSalt: String = UUID.randomUUID().toString
}
