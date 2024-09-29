package com.holodome.tests.services

import java.time.Instant

import com.holodome.domain.errors.ChatAccessForbidden
import com.holodome.domain.services.*
import com.holodome.effects.TimeSource
import com.holodome.interpreters.*
import com.holodome.tests.generators.{ createAdRequestGen, registerGen, sendMessageRequestGen }
import com.holodome.tests.repositories.*
import com.holodome.tests.repositories.inmemory.InMemoryRepositoryFactory
import com.holodome.tests.repositories.stubs.RepositoryStubFactory
import com.holodome.tests.services.stubs.TelemetryServiceStub

import cats.effect.IO
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object MessageServiceSuite extends SimpleIOSuite with Checkers:

  given Logger[IO] = NoOpLogger[IO]

  private val epoch: Long = 1711564995
  private given TimeSource[IO] = new:
    def instant: IO[Instant] = Instant.ofEpochSecond(epoch).pure[IO]

  def makeTestServies: (UserService[F], AdService[F], ChatService[F], MessageService[F]) =
    val telemetry      = new TelemetryServiceStub[IO]
    val userRepo       = InMemoryRepositoryFactory.users
    val adRepo         = InMemoryRepositoryFactory.ads
    val chatRepo       = InMemoryRepositoryFactory.chats
    val msgRepo        = InMemoryRepositoryFactory.msgs
    val iam            = IAMServiceInterpreter.make(adRepo, chatRepo, RepositoryStubFactory.images)
    val users          = UserServiceInterpreter.make[IO](userRepo, adRepo, iam)
    val feedRepository = RepositoryStubFactory.feed
    val ads            = AdServiceInterpreter.make[IO](adRepo, RepositoryStubFactory.tags, feedRepository, iam, telemetry)
    val chats          = ChatServiceInterpreter.make[IO](chatRepo, adRepo, telemetry, iam)
    val msgs           = MessageServiceInterpreter.make[IO](msgRepo, iam)
    (users, ads, chats, msgs)

  test("basic message works") {
    val (users, ads, chats, msgs) = makeTestServies
    val gen =
      for
        reg      <- registerGen
        otherReg <- registerGen
        ad       <- createAdRequestGen
        msg      <- sendMessageRequestGen
      yield (reg, otherReg, ad, msg)
    forall(gen) { case (reg, otherReg, createAd, msg) =>
      for
        u1      <- users.create(reg)
        u2      <- users.create(otherReg)
        ad      <- ads.create(u1, createAd)
        chat    <- chats.create(ad, u2)
        _       <- msgs.send(chat, u2, msg)
        history <- msgs.history(chat, u2)
      yield matches(history.messages) { case List(m) =>
        expect.all(
          m.text === msg.text,
          m.chat === chat,
          m.at.getEpochSecond === epoch,
          m.sender === u2
        )
      }
    }
  }

  test("unable to send to forbidden chat") {
    val (users, ads, chats, msgs) = makeTestServies
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
        ad   <- ads.create(u1, createAd)
        chat <- chats.create(ad, u2)
        x <- msgs
          .send(chat, u3, msg)
          .map(Some(_))
          .recoverWith { case ChatAccessForbidden(_) =>
            None.pure[IO]
          }
      yield expect.all(x.isEmpty)
    }
  }

  test("unable to gen history for forbidden chat") {
    val (users, ads, chats, msgs) = makeTestServies
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
        ad   <- ads.create(u1, createAd)
        chat <- chats.create(ad, u2)
        _    <- msgs.send(chat, u2, msg)
        _ <- msgs
          .history(chat, u3)
          .map(Some(_))
          .recoverWith { case ChatAccessForbidden(_) =>
            None.pure[IO]
          }
      yield expect.all(true)
    }
  }
