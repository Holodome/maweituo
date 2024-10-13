package maweituo
package tests
package e2e

import maweituo.http.dto.*
import maweituo.tests.e2e.resources.app

import dev.profunktor.auth.jwt.JwtToken
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.{Method, Request}
import weaver.*
import weaver.scalacheck.{CheckConfig, Checkers}

class AppE2ESuite(global: GlobalRead) extends ResourceSuite:

  override def maxParallelism: Int = 1
  override def checkConfig: CheckConfig =
    CheckConfig.default.copy(minimumSuccessful = 1, perPropertyParallelism = 1)

  type Res = AppClient

  override def sharedResource: Resource[IO, Res] = global.app

  e2eTest("create ad with tag") { (client, log) =>
    val gen =
      for
        r   <- registerGen
        ad  <- adTitleGen
        tag <- adTagGen
      yield (r, ad, tag)
    forall(gen) { (reg, adTitle, tag) =>
      for
        _   <- client.register(RegisterRequestDto(reg.name, reg.email, reg.password))
        jwt <- client.login(LoginRequestDto(reg.name, reg.password)).map(_.jwt)
        given JwtToken = jwt
        adId <- client.createAd(CreateAdRequestDto(adTitle)).map(_.id)
        _    <- client.addTag(adId, AddTagRequestDto(tag))
        ad   <- client.getAd(adId)
        tags <- client.getAdTags(adId).map(_.tags)
      yield expect.same(ad.title, adTitle) and expect.same(List(tag), tags)
    }
  }

  e2eTest("domain errors") { (client, log) =>
    forall(registerGen) { reg =>
      val r = RegisterRequestDto(reg.name, reg.email, reg.password)
      for
        _ <- client.register(r)
        x <- client.client.fetchAs[ErrorResponseDto](
          Request[IO](
            method = Method.POST,
            uri = client.makeUri("register")
          ).withEntity(r)
        )
      yield expect.same(ErrorResponseDto(List(s"email ${reg.email} is already taken")), x)
    }
  }

  e2eTest("unauthorized routes") { (client, log) =>
    forall(adTitleGen) { (title) =>
      for
        x <- client.client.status(
          Request[IO](
            method = Method.POST,
            uri = client.makeUri("ads")
          ).withEntity(CreateAdRequestDto(title))
        )
      yield expect.same(org.http4s.Status.Unauthorized, x)
    }
  }
