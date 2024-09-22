package com.holodome.auth

import java.security.MessageDigest

import com.holodome.domain.users.*
import com.holodome.effects.GenUUID

import cats.Functor
import cats.syntax.all.*

object PasswordHashing:
  def hashSaltPassword(
      password: Password,
      salt: PasswordSalt
  ): HashedSaltedPassword = HashedSaltedPassword(sha256(password.value + salt.value))

  def genSalt[F[_]: GenUUID: Functor]: F[PasswordSalt] =
    GenUUID[F].make map { uuid => PasswordSalt(uuid.toString) }

  private def sha256(string: String): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(string.getBytes("UTF-8"))
      .map("%02x".format(_))
      .mkString
