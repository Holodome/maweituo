package maweituo.tests.properties.services

import cats.effect.IO
import cats.syntax.all.*

import maweituo.domain.Identity
import maweituo.domain.ads.services.{AdService, ChatService}
import maweituo.domain.errors.*
import maweituo.domain.users.services.UserService
import maweituo.tests.generators.*
import maweituo.tests.utils.given

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
            given Identity = Identity(user)
            x <- chats.get(chat).attempt
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
            given Identity = Identity(u1)
            ad <- ads.create(createAd)
            _  <- chats.create(ad)(using Identity(u2))
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
            given Identity = Identity(u1)
            ad <- ads.create(createAd)
            x  <- chats.create(ad).attempt
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
            ad <- ads.create(createAd)(using Identity(u1))
            given Identity = Identity(u2)
            _ <- chats.create(ad)
            x <- chats.create(ad).attempt
          yield expect.same(Left(ChatAlreadyExists(ad, u2)), x)
        }
    )
  )
