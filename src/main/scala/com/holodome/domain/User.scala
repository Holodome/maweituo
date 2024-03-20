package com.holodome.domain

import com.holodome.domain.auth._

import java.util.UUID
import java.time.Instant

case class User(
    id: UUID,
    name: String,
    email: String,
    hashedPassword: HashedSaltedPassword,
    salt: PasswordSalt,
    createdAt: Instant,
    updatedAt: Instant
)

object User {
  case class CreateUser(
      name: String,
      email: String,
      password: HashedSaltedPassword,
      salt: PasswordSalt,
      time: Instant
  )
}
