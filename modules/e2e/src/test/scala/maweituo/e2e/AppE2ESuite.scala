package maweituo.e2e

import cats.effect.IO
import cats.effect.kernel.Resource

import maweituo.e2e.resources.app
import maweituo.http.dto.{AddTagRequestDto, CreateAdRequestDto, ErrorResponseDto, LoginRequestDto, RegisterRequestDto}
import maweituo.tests.ResourceSuite
import maweituo.tests.generators.*
import maweituo.tests.utils.given

import dev.profunktor.auth.jwt.JwtToken
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.{Method, Request}
import weaver.*
import weaver.scalacheck.{CheckConfig, Checkers}
import cats.data.NonEmptyList

class AppE2ESuite(global: GlobalRead) extends ResourceSuite:

  override def maxParallelism: Int = 1
  override def checkConfig: CheckConfig =
    CheckConfig.default.copy(minimumSuccessful = 1, maximumGeneratorSize = 1, perPropertyParallelism = 1)

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
      yield expect.same(ErrorResponseDto(NonEmptyList.one(s"email ${reg.email} is already taken")), x)
    }
  }
