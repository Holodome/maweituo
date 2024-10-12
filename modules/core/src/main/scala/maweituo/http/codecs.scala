package maweituo
package http

import maweituo.domain.ads.AdTag
import maweituo.utils.IsUUID

import dev.profunktor.auth.jwt.JwtToken
import sttp.tapir.{Codec, CodecFormat, Schema}

object codecs:
  given [A: IsUUID]: Codec[String, A, CodecFormat.TextPlain] =
    val u = summon[IsUUID[A]]
    Codec.uuid.map[A](u.iso.get)(u.iso.reverseGet)

  given Codec[String, AdTag, CodecFormat.TextPlain] =
    Codec.string.map(AdTag.apply)(_.value)

  given Schema[JwtToken] = Schema.string[JwtToken]
