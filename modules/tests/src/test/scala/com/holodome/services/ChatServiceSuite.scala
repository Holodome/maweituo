package com.holodome.services

import cats.effect.IO
import cats.syntax.all._
import com.holodome.domain.errors.{CannotCreateChatWithMyself, ChatAlreadyExists}
import com.holodome.generators.{createAdRequestGen, registerGen}
import com.holodome.repositories._
import org.mockito.MockitoSugar.mock
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object ChatServiceSuite extends SimpleIOSuite with Checkers {
  private def makeIam(ad: AdvertisementRepository[IO], chat: ChatRepository[F]): IAMService[IO] =
    IAMService.make(ad, chat, mock[AdImageRepository[IO]])

  private val telemetry: TelemetryService[IO] = new TelemetryServiceStub[IO]

  test("create works") {
    val gen = for {
      reg      <- registerGen
      otherReg <- registerGen
      ad       <- createAdRequestGen
    } yield (reg, otherReg, ad)
    forall(gen) { case (reg, otherReg, createAd) =>
      val userRepo = new InMemoryUserRepository[IO]
      val adRepo   = new InMemoryAdRepository[IO]
      val chatRepo = new InMemoryChatRepository[IO]
      val iam      = makeIam(adRepo, chatRepo)
      val users    = UserService.make[IO](userRepo, iam)
      val ads      = AdvertisementService.make[IO](adRepo, mock[TagRepository[IO]], iam)
      val chats    = ChatService.make[IO](chatRepo, adRepo, telemetry)
      for {
        u1 <- users.create(reg)
        u2 <- users.create(otherReg)
        ad <- ads.create(u1, createAd)
        _  <- chats.create(ad, u2)
      } yield expect.all(true)
    }
  }

  test("can't create chat with myself") {
    val gen = for {
      reg <- registerGen
      ad  <- createAdRequestGen
    } yield (reg, ad)
    forall(gen) { case (reg, createAd) =>
      val userRepo = new InMemoryUserRepository[IO]
      val adRepo   = new InMemoryAdRepository[IO]
      val chatRepo = new InMemoryChatRepository[IO]
      val iam      = makeIam(adRepo, chatRepo)
      val users    = UserService.make[IO](userRepo, iam)
      val ads      = AdvertisementService.make[IO](adRepo, mock[TagRepository[IO]], iam)
      val chats    = ChatService.make[IO](chatRepo, adRepo, telemetry)
      for {
        u1 <- users.create(reg)
        ad <- ads.create(u1, createAd)
        x <- chats
          .create(ad, u1)
          .map(Some(_))
          .recoverWith { case CannotCreateChatWithMyself() =>
            None.pure[IO]
          }
      } yield expect.all(x.isEmpty)
    }
  }

  test("can't create same chat multiple times") {
    val gen = for {
      reg      <- registerGen
      otherReg <- registerGen
      ad       <- createAdRequestGen
    } yield (reg, otherReg, ad)
    forall(gen) { case (reg, otherReg, createAd) =>
      val userRepo = new InMemoryUserRepository[IO]
      val adRepo   = new InMemoryAdRepository[IO]
      val chatRepo = new InMemoryChatRepository[IO]
      val iam      = makeIam(adRepo, chatRepo)
      val users    = UserService.make[IO](userRepo, iam)
      val ads      = AdvertisementService.make[IO](adRepo, mock[TagRepository[IO]], iam)
      val chats    = ChatService.make[IO](chatRepo, adRepo, telemetry)
      for {
        u1 <- users.create(reg)
        u2 <- users.create(otherReg)
        ad <- ads.create(u1, createAd)
        _  <- chats.create(ad, u2)
        x <- chats
          .create(ad, u2)
          .map(Some(_))
          .recoverWith { case ChatAlreadyExists() =>
            None.pure[IO]
          }
      } yield expect.all(x.isEmpty)
    }
  }
}
