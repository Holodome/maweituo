package maweituo.tests.misc
import maweituo.utils.given

import dev.profunktor.auth.jwt.JwtToken
import io.circe.testing.{ArbitraryInstances, CodecTests}
import org.scalacheck.{Arbitrary, Gen}
import weaver.*
import weaver.discipline.*

object JwtTokenCodecSuite extends FunSuite with Discipline with ArbitraryInstances:
  given Arbitrary[JwtToken] = Arbitrary {
    Gen.listOf(Gen.alphaChar) map { chars => JwtToken(chars.mkString("")) }
  }

  checkAll("JwtToken codec", CodecTests[JwtToken].codec)
