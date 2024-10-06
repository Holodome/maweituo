package maweituo.tests.properties.services

import java.time.Instant

import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.all.*

import maweituo.domain.Identity
import maweituo.domain.ads.services.*
import maweituo.domain.errors.*
import maweituo.domain.users.UserId
import maweituo.domain.users.services.UserService
import maweituo.effects.TimeSource
import maweituo.tests.generators.*

import weaver.scalacheck.Checkers
import weaver.{Expectations, MutableIOSuite}

trait MessageServiceProperties:
  this: MutableIOSuite & Checkers =>

  protected final case class Property(
      name: String,
      exp: (UserService[IO], AdService[IO], ChatService[IO], MessageService[IO]) => IO[Expectations]
  )

  protected val epoch: Long = 1711564995
  protected given timeSource: TimeSource[IO] = new:
    def instant: IO[Instant] = Instant.ofEpochSecond(epoch).pure[IO]

  protected val properties = List(
    Property(
      "empty chat is empty",
      (users, ads, chats, msgs) =>
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
            chat    <- chats.create(ad)
            history <- msgs.history(chat)
          yield expect.all(history.messages.isEmpty)
        }
    ),
    Property(
      "basic message works",
      (users, ads, chats, msgs) =>
        val gen =
          for
            reg      <- registerGen
            otherReg <- registerGen
            ad       <- createAdRequestGen
            msg      <- sendMessageRequestGen
          yield (reg, otherReg, ad, msg)
        forall(gen) { case (reg, otherReg, createAd, msg) =>
          for
            u1 <- users.create(reg)
            u2 <- users.create(otherReg)
            ad <- ads.create(createAd)(using Identity(u1))
            given Identity = Identity(u2)
            chat    <- chats.create(ad)
            _       <- msgs.send(chat, msg)
            history <- msgs.history(chat)
          yield matches(history.messages) { case List(m) =>
            NonEmptyList.of(
              expect.same(m.text, msg.text),
              expect.same(m.chat, chat),
              expect.same(m.at.getEpochSecond, epoch),
              expect.same(m.sender, u2)
            ).reduce
          }
        }
    ),
    Property(
      "unable to send to forbidden chat",
      (users, ads, chats, msgs) =>
        val gen =
          for
            reg1 <- registerGen
            reg2 <- registerGen
            reg3 <- registerGen
            ad   <- createAdRequestGen
            msg  <- sendMessageRequestGen
          yield (reg1, reg2, reg3, ad, msg)
        forall(gen) { case (reg1, reg2, reg3, createAd, msg) =>
          for
            u1   <- users.create(reg1)
            u2   <- users.create(reg2)
            u3   <- users.create(reg3)
            ad   <- ads.create(createAd)(using Identity(u1))
            chat <- chats.create(ad)(using Identity(u2))
            x    <- msgs.send(chat, msg)(using Identity(u3)).attempt
          yield expect.same(Left(ChatAccessForbidden(chat)), x)
        }
    ),
    Property(
      "unable to gen history for forbidden chat",
      (users, ads, chats, msgs) =>
        val gen =
          for
            reg1 <- registerGen
            reg2 <- registerGen
            reg3 <- registerGen
            ad   <- createAdRequestGen
            msg  <- sendMessageRequestGen
          yield (reg1, reg2, reg3, ad, msg)
        forall(gen) { case (reg1, reg2, reg3, createAd, msg) =>
          for
            u1   <- users.create(reg1)
            u2   <- users.create(reg2)
            u3   <- users.create(reg3)
            ad   <- ads.create(createAd)(using Identity(u1))
            chat <- chats.create(ad)(using Identity(u2))
            _    <- msgs.send(chat, msg)(using Identity(u2))
            x    <- msgs.history(chat)(using Identity(u3)).attempt
          yield expect.same(Left(ChatAccessForbidden(chat)), x)
        }
    )
  )
