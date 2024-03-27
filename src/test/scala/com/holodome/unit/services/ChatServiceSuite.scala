package com.holodome.unit.services

import cats.effect.IO
import cats.syntax.all._
import com.holodome.domain.advertisements.{CannotCreateChatWithMyself, ChatAlreadyExists}
import com.holodome.repositories.{AdvertisementRepository, ChatRepository, ImageRepository}
import com.holodome.services.{AdvertisementService, ChatService, IAMService, UserService}
import com.holodome.unit.services.AdvertisementServiceSuite.{expect, test}
import com.holodome.utils.generators.{createAdRequestGen, registerGen}
import com.holodome.utils.repositories.{
  InMemoryAdRepository,
  InMemoryChatRepository,
  InMemoryUserRepository
}
import org.mockito.MockitoSugar.mock
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object ChatServiceSuite extends SimpleIOSuite with Checkers {
  private def makeIam(ad: AdvertisementRepository[IO], chat: ChatRepository[F]): IAMService[IO] =
    IAMService.make(ad, chat, mock[ImageRepository[IO]])

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
      val ads      = AdvertisementService.make[IO](adRepo, iam)
      val chats    = ChatService.make[IO](chatRepo, ads)
      for {
        u1 <- users.register(reg)
        u2 <- users.register(otherReg)
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
      val ads      = AdvertisementService.make[IO](adRepo, iam)
      val chats    = ChatService.make[IO](chatRepo, ads)
      for {
        u1 <- users.register(reg)
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
      val ads      = AdvertisementService.make[IO](adRepo, iam)
      val chats    = ChatService.make[IO](chatRepo, ads)
      for {
        u1 <- users.register(reg)
        u2 <- users.register(otherReg)
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
