package com.holodome.domain

import java.util.UUID
import java.time.Instant

case class User(
    id: UUID,
    name: String,
    email: String,
    hashedPassword: String,
    salt: String,
    createdAt: Instant,
    updatedAt: Instant
)

object User {
  case class CreateUser(name: String, email: String, password: String, salt: String, time: Instant)
}
