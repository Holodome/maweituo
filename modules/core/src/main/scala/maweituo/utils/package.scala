package maweituo
package utils

import java.time.Instant

import scala.concurrent.duration.{Duration, FiniteDuration}

import cats.syntax.all.*
import cats.{Eq, Show}

import dev.profunktor.auth.jwt.JwtToken
import io.circe.{Decoder, Encoder}

given Eq[JwtToken]      = Eq.by(_.value)
given Encoder[JwtToken] = Encoder.forProduct1("access_token")(_.value)
given Decoder[JwtToken] = Decoder.forProduct1("access_token")(JwtToken.apply)

given Show[Instant] = Show[String].contramap[Instant](_.toString())

given Decoder[FiniteDuration] =
  Decoder[String].emap { s =>
    Duration(s) match
      case fd: FiniteDuration => fd.asRight
      case e                  => e.toString.asLeft
  }

given Encoder[FiniteDuration] =
  Encoder[String].contramap(_.toString)
