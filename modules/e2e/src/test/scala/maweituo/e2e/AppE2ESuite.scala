package maweituo.e2e

import cats.effect.IO
import cats.effect.kernel.Resource

import maweituo.domain.ads.{AddTagRequest, CreateAdRequest}
import maweituo.domain.users.LoginRequest
import maweituo.e2e.resources.app
import maweituo.tests.ResourceSuite
import maweituo.tests.generators.*

import dev.profunktor.auth.jwt.JwtToken
import weaver.*
import weaver.scalacheck.Checkers

class AppE2ESuite(global: GlobalRead) extends ResourceSuite:

  type Res = AppClient

  override def sharedResource: Resource[IO, Res] = global.app

  e2eTest("create ad with tag") { client =>
    val gen =
      for
        r   <- registerGen
        ad  <- adTitleGen
        tag <- adTagGen
      yield (r, ad, tag)
    forall(gen) { (reg, adTitle, tag) =>
      for
        _   <- client.register(reg)
        jwt <- client.login(LoginRequest(reg.name, reg.password))
        given JwtToken = jwt
        adId <- client.createAd(CreateAdRequest(adTitle))
        _    <- client.addTag(adId, AddTagRequest(tag))
        ad   <- client.getAd(adId)
        tags <- client.getAdTags(adId)
      yield expect.same(ad.title, adTitle) and expect.same(List(tag), tags)
    }
  }
