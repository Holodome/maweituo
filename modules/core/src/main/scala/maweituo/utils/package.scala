package maweituo.utils

import java.time.Instant

import scala.concurrent.duration.{ Duration, FiniteDuration }

import cats.*
import cats.syntax.all.*
import dev.profunktor.auth.jwt.JwtToken
import io.circe.{ Decoder, Encoder }

given Eq[JwtToken]      = Eq.by(_.value)
given Show[JwtToken]    = Show[String].contramap[JwtToken](_.value)
given Encoder[JwtToken] = Encoder.forProduct1("access_token")(_.value)

given Show[Instant] = Show[String].contramap[Instant](_.toString())

given Decoder[FiniteDuration] =
  Decoder[String].emap { s =>
    Duration(s) match
      case fd: FiniteDuration => fd.asRight
      case e                  => e.toString.asLeft
  }

given Encoder[FiniteDuration] =
  Encoder[String].contramap(_.toString)
