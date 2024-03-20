package com.holodome.domain

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.circe._
import io.circe._
import io.circe.syntax._
import io.circe.parser._

import scala.util.control.NoStackTrace

object auth {
  @derive(decoder, encoder)
  final case class LoginRequest(name: String, password: String)

  @derive(decoder, encoder)
  final case class RegisterRequest(
      name: String,
      email: String,
      password: String
  )

  case class NoUserFound(username: String) extends NoStackTrace
  case class UserNameInUse(username: String) extends NoStackTrace
  case class UserEmailInUse(email: String) extends NoStackTrace
  case class InvalidPassword(username: String) extends NoStackTrace
}
