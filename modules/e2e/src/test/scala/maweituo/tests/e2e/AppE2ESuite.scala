package maweituo
package tests
package e2e

import maweituo.http.dto.*
import maweituo.tests.e2e.MaweituoApiClient.unwrap
import maweituo.tests.e2e.resources.app

import dev.profunktor.auth.jwt.JwtToken
import sttp.model.StatusCode
import weaver.*
import weaver.scalacheck.{CheckConfig, Checkers}

class AppE2ESuite(global: GlobalRead) extends ResourceSuite:

  override def maxParallelism: Int = 1
  override def checkConfig: CheckConfig =
    CheckConfig.default.copy(minimumSuccessful = 1, perPropertyParallelism = 1)

  type Res = MaweituoApiClient

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
        _   <- client.`post /register`(RegisterRequestDto(reg.name, reg.email, reg.password))
        jwt <- client.`post /login`(LoginRequestDto(reg.name, reg.password)).unwrap
        given JwtToken = jwt.jwt
        adId <- client.`post /ads`(CreateAdRequestDto(adTitle)).unwrap.map(_.id)
        _    <- client.`post /ads/$adId/tags`(adId, AddTagRequestDto(tag))
        ad   <- client.`get /ads/$adId`(adId).unwrap
        tags <- client.`get /ads/$adId/tags`(adId).unwrap.map(_.tags)
      yield expect.same(ad.title, adTitle) and expect.same(List(tag), tags)
    }
  }

  e2eTest("domain errors") { (client, log) =>
    forall(registerGen) { reg =>
      val r = RegisterRequestDto(reg.name, reg.email, reg.password)
      for
        _ <- client.`post /register`(r)
        x <- client.`post /register`(r)
      yield expect.same(Left(ErrorResponseDto(List(s"email ${reg.email} is already taken"))), x.leftMap(_._2))
    }
  }

  e2eTest("unauthorized routes") { (client, log) =>
    forall(adTitleGen) { (title) =>
      given JwtToken = JwtToken("aboba")
      for
        x <- client.`post /ads`(CreateAdRequestDto(title))
      yield expect.same(Left(StatusCode.Unauthorized), x.leftMap(_._1))
    }
  }
