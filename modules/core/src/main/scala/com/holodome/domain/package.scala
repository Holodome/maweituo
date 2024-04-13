package com.holodome

import cats.{Eq, Show}
import cats.syntax.all._
import dev.profunktor.auth.jwt.JwtToken
import io.circe.Encoder

package object domain extends OrphanInstances

trait OrphanInstances {
  implicit val tokenEq: Eq[JwtToken]        = Eq.by(_.value)
  implicit val jwtTokenShow: Show[JwtToken] = Show[String].contramap[JwtToken](_.value)
  implicit val tokenEncoder: Encoder[JwtToken] =
    Encoder.forProduct1("access_token")(_.value)
}
