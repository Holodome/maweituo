package com.holodome.models.auth

import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._

object Login {
  case class Request(email: String, password: String)
}