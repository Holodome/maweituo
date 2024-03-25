package com.holodome.auth

import cats.syntax.all._
import cats.Functor
import com.holodome.domain.users._
import com.holodome.effects.GenUUID

import java.security.MessageDigest

object PasswordHashing {
  def hashSaltPassword(
      password: Password,
      salt: PasswordSalt
  ): HashedSaltedPassword = HashedSaltedPassword(sha256(password.value + salt.value))

  def genSalt[F[_]: GenUUID: Functor]: F[PasswordSalt] =
    GenUUID[F].make map { uuid => PasswordSalt(uuid.toString) }

  private lazy val digest = MessageDigest.getInstance("SHA-256")
  private def sha256(string: String): String =
    digest
      .digest(string.getBytes("UTF-8"))
      .map("%02x".format(_))
      .mkString
}
