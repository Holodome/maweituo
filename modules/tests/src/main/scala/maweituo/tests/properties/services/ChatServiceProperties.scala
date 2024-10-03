package maweituo.tests.properties.services

import cats.effect.IO
import cats.syntax.all.*

import maweituo.domain.ads.services.{AdService, ChatService}
import maweituo.domain.errors.*
import maweituo.domain.users.services.UserService
import maweituo.tests.generators.*

import weaver.scalacheck.Checkers
import weaver.{Expectations, MutableIOSuite}

trait ChatServiceProperties:
  this: MutableIOSuite & Checkers =>

  protected final case class Property(
      name: String,
      exp: (UserService[IO], AdService[IO], ChatService[IO]) => IO[Expectations]
  )

  protected val properties = List(
    Property(
      "invalid chat id",
      (users, ads, chats) =>
        val gen =
          for
            u <- registerGen
            c <- chatIdGen
          yield u -> c
        forall(gen) { (reg, chat) =>
          for
            user <- users.create(reg)
            x    <- chats.get(chat, user).attempt
          yield expect.same(Left(InvalidChatId(chat)), x)
        }
    ),
    Property(
      "create works",
      (users, ads, chats) =>
        val gen =
          for
            reg      <- registerGen
            otherReg <- registerGen
            ad       <- createAdRequestGen
          yield (reg, otherReg, ad)
        forall(gen) { case (reg, otherReg, createAd) =>
          for
            u1 <- users.create(reg)
            u2 <- users.create(otherReg)
            ad <- ads.create(u1, createAd)
            _  <- chats.create(ad, u2)
          yield success
        }
    ),
    Property(
      "can't create chat with myself",
      (users, ads, chats) =>
        val gen =
          for
            reg <- registerGen
            ad  <- createAdRequestGen
          yield (reg, ad)
        forall(gen) { case (reg, createAd) =>
          for
            u1 <- users.create(reg)
            ad <- ads.create(u1, createAd)
            x  <- chats.create(ad, u1).attempt
          yield expect.same(Left(CannotCreateChatWithMyself(ad, u1)), x)
        }
    ),
    Property(
      "can't create same chat multiple times",
      (users, ads, chats) =>
        val gen =
          for
            reg      <- registerGen
            otherReg <- registerGen
            ad       <- createAdRequestGen
          yield (reg, otherReg, ad)
        forall(gen) { case (reg, otherReg, createAd) =>
          for
            u1 <- users.create(reg)
            u2 <- users.create(otherReg)
            ad <- ads.create(u1, createAd)
            _  <- chats.create(ad, u2)
            x  <- chats.create(ad, u2).attempt
          yield expect.same(Left(ChatAlreadyExists(ad, u2)), x)
        }
    )
  )
