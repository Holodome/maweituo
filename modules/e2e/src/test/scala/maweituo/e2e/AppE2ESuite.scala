package maweituo.e2e

import cats.effect.IO
import cats.effect.kernel.Resource

import maweituo.e2e.resources.app
import maweituo.http.dto.{AddTagRequestDto, CreateAdRequestDto, LoginRequestDto, RegisterRequestDto}
import maweituo.tests.generators.*
import maweituo.tests.{ResourceSuite, WeaverLogAdapter}
import maweituo.tests.utils.given

import dev.profunktor.auth.jwt.JwtToken
import org.typelevel.log4cats.Logger
import weaver.*
import weaver.scalacheck.{CheckConfig, Checkers}

class AppE2ESuite(global: GlobalRead) extends ResourceSuite:

  override def maxParallelism: Int = 1
  override def checkConfig: CheckConfig =
    CheckConfig.default.copy(minimumSuccessful = 1, maximumGeneratorSize = 1, perPropertyParallelism = 1)

  type Res = AppClient

  override def sharedResource: Resource[IO, Res] = global.app

  e2eTest("create ad with tag") { (client, log) =>
    given Logger[IO] = WeaverLogAdapter(log)
    val gen =
      for
        r   <- registerGen
        ad  <- adTitleGen
        tag <- adTagGen
      yield (r, ad, tag)
    forall(gen) { (reg, adTitle, tag) =>
      for
        _   <- client.register(RegisterRequestDto(reg.name, reg.email, reg.password))
        _   <- Logger[IO].info("register done")
        jwt <- client.login(LoginRequestDto(reg.name, reg.password)).map(_.jwt)
        _   <- Logger[IO].info("login done")
        given JwtToken = jwt
        adId <- client.createAd(CreateAdRequestDto(adTitle)).map(_.id)
        _    <- Logger[IO].info("create ad done")
        _    <- client.addTag(adId, AddTagRequestDto(tag))
        _    <- Logger[IO].info("add tag done")
        ad   <- client.getAd(adId)
        _    <- Logger[IO].info("get ad done")
        tags <- client.getAdTags(adId).map(_.tags)
        _    <- Logger[IO].info("get ad tags done")
      yield expect.same(ad.title, adTitle) and expect.same(List(tag), tags)
    }
  }
