package maweituo
package tests
package e2e

import cats.syntax.all.*
import maweituo.http.dto.*
import maweituo.tests.e2e.resources.app
import maweituo.tests.e2e.resources.AppCon

import dev.profunktor.auth.jwt.JwtToken
import sttp.model.StatusCode
import weaver.*
import weaver.scalacheck.{CheckConfig, Checkers}

class AppE2ESuite(global: GlobalRead) extends ResourceSuite:

  override def maxParallelism: Int = 1
  override def checkConfig: CheckConfig =
    CheckConfig.default.copy(minimumSuccessful = 1, perPropertyParallelism = 1)

  type Res = AppCon

  override def sharedResource: Resource[IO, Res] = global.app

  def clientTest(name: String)(fn: MaweituoApiClient => IO[Expectations]) =
    e2eTest(name) { (con, log) =>
      given LoggerFactory[IO] = WeaverLogAdapterFactory[IO](log)
      val client              = MaweituoApiClient(con.base, con.client)
      fn(client)
    }

  clientTest("create ad with tag") { client =>
    import client.*
    val gen =
      for
        r   <- registerGen
        ad  <- adTitleGen
        tag <- adTagGen
      yield (r, ad, tag)
    forall(gen) { (reg, adTitle, tag) =>
      for
        _   <- `post /register`(RegisterRequestDto.fromDomain(reg))
        jwt <- `post /login`(LoginRequestDto(reg.name, reg.password))
        given JwtToken = jwt.jwt
        adId <- `post /ads`(CreateAdRequestDto(adTitle)).map(_.id)
        _    <- `post /ads/$adId/tags`(adId, AddTagRequestDto(tag))
        ad   <- `get /ads/$adId`(adId)
        tags <- `get /ads/$adId/tags`(adId).map(_.tags)
      yield expect.same(ad.title, adTitle) and expect.same(List(tag), tags)
    }
  }

  clientTest("chats") { client =>
    import client.*
    val gen =
      for
        reg      <- registerGen
        otherReg <- registerGen
        ad       <- createAdRequestGen
        msg      <- sendMessageRequestGen
      yield (reg, otherReg, ad, msg)
    forall(gen) { case (reg, otherReg, createAd, msg) =>
      for
        u1 <- `post /register`(RegisterRequestDto.fromDomain(reg))
        u2 <- `post /register`(RegisterRequestDto.fromDomain(otherReg)).map(_.userId)
        adId <-
          for
            jwt <- `post /login`(LoginRequestDto(reg.name, reg.password))
            given JwtToken = jwt.jwt
            adId <- `post /ads`(CreateAdRequestDto.fromDomain(createAd)).map(_.id)
          yield adId
        jwt <- `post /login`(LoginRequestDto(otherReg.name, otherReg.password))
        given JwtToken = jwt.jwt
        chatId  <- `post /ads/$adId/chats`(adId).map(_.chatId)
        _       <- `post /ads/$adId/chats/$chatId/msgs`(adId, chatId, SendMessageRequestDto(msg.text))
        history <- `get /ads/$adId/chats/$chatId/msgs`(adId, chatId, 0, None).map(_.messages.items)
      yield matches(history) { case List(m) =>
        NonEmptyList.of(
          expect.same(m.text, msg.text),
          expect.same(m.chatId, chatId),
          expect.same(m.senderId, u2)
        ).reduce
      }
    }
  }

  clientTest("domain errors") { client =>
    import client.*
    forall(registerGen) { reg =>
      val r = RegisterRequestDto(reg.name, reg.email, reg.password)
      for
        _ <- `post /register`(r)
        x <- `post /register`.attempt(r)
      yield expect.same(Left(ErrorResponseDto(List(s"email ${reg.email} is already taken"))), x.leftMap(_._2))
    }
  }

  clientTest("unauthorized routes") { client =>
    import client.*
    forall(adTitleGen) { (title) =>
      given JwtToken = JwtToken("aboba")
      for
        x <- `post /ads`.attempt(CreateAdRequestDto(title))
      yield expect.same(Left(StatusCode.Unauthorized), x.leftMap(_._1))
    }
  }
