package com.holodome

import cats._
import cats.syntax.all._
import dev.profunktor.auth.jwt.JwtToken
import io.circe.Encoder

package object utils extends OrphanInstances {
  case class RefinedEncodingFailure(reason: String) extends Exception(reason)

  type EncodeR[T, A] = EncodeRF[Either[Throwable, _], T, A]
  object EncodeR {
    def apply[T, A](implicit I: EncodeR[T, A]): EncodeR[T, A] = I
  }
}

sealed trait OrphanInstances {
  implicit val tokenEq: Eq[JwtToken]        = Eq.by(_.value)
  implicit val jwtTokenShow: Show[JwtToken] = Show[String].contramap[JwtToken](_.value)
  implicit val tokenEncoder: Encoder[JwtToken] =
    Encoder.forProduct1("access_token")(_.value)
}
