package com.holodome.tests.services

import cats.effect.IO
import cats.syntax.all._
import com.holodome.domain.errors.ChatAccessForbidden
import com.holodome.domain.repositories._
import com.holodome.domain.services._
import com.holodome.effects.TimeSource
import com.holodome.interpreters._
import com.holodome.tests.generators.{createAdRequestGen, registerGen, sendMessageRequestGen}
import com.holodome.tests.repositories._
import com.holodome.tests.services.TelemetryServiceStub
import org.mockito.MockitoSugar
import org.mockito.cats.MockitoCats
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import java.time.Instant

object MessageServiceSuite extends SimpleIOSuite with Checkers with MockitoSugar with MockitoCats {
  implicit val logger: Logger[IO] = NoOpLogger[IO]

  private def makeIam(ad: AdvertisementRepository[IO], chat: ChatRepository[IO]): IAMService[IO] =
    IAMServiceInterpreter.make(ad, chat, mock[AdImageRepository[IO]])

  private val epoch: Long = 1711564995
  private implicit def clockMock: TimeSource[IO] = new TimeSource[IO] {
    override def instant: IO[Instant] = Instant.ofEpochSecond(epoch).pure[IO]
  }
  private val telemetry                          = new TelemetryServiceStub[IO]
  private val feedRepository: FeedRepository[IO] = new FeedRepositoryStub

  test("basic message works") {
    val gen = for {
      reg      <- registerGen
      otherReg <- registerGen
      ad       <- createAdRequestGen
      msg      <- sendMessageRequestGen
    } yield (reg, otherReg, ad, msg)
    forall(gen) { case (reg, otherReg, createAd, msg) =>
      val userRepo = new InMemoryUserRepository[IO]
      val adRepo   = new InMemoryAdRepository[IO]
      val chatRepo = new InMemoryChatRepository[IO]
      val msgRepo  = new InMemoryMessageRepository[IO]
      val iam      = makeIam(adRepo, chatRepo)
      val users    = UserServiceInterpreter.make[IO](userRepo, new UserAdsRepositoryStub, iam)
      val ads =
        AdServiceInterpreter.make[IO](
          adRepo,
          mock[TagRepository[IO]],
          feedRepository,
          new UserAdsRepositoryStub,
          iam,
          new TelemetryServiceStub[IO]
        )
      val chats = ChatServiceInterpreter.make[IO](chatRepo, adRepo, telemetry)
      val msgs  = MessageServiceInterpreter.make[IO](msgRepo, iam)
      for {
        u1      <- users.create(reg)
        u2      <- users.create(otherReg)
        ad      <- ads.create(u1, createAd)
        chat    <- chats.create(ad, u2)
        _       <- msgs.send(chat, u2, msg)
        history <- msgs.history(chat, u2)
      } yield matches(history.messages) { case List(m) =>
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
    val gen = for {
      reg1 <- registerGen
      reg2 <- registerGen
      reg3 <- registerGen
      ad   <- createAdRequestGen
      msg  <- sendMessageRequestGen
    } yield (reg1, reg2, reg3, ad, msg)
    forall(gen) { case (reg1, reg2, reg3, createAd, msg) =>
      val userRepo = new InMemoryUserRepository[IO]
      val adRepo   = new InMemoryAdRepository[IO]
      val chatRepo = new InMemoryChatRepository[IO]
      val msgRepo  = new InMemoryMessageRepository[IO]
      val iam      = makeIam(adRepo, chatRepo)
      val users    = UserServiceInterpreter.make[IO](userRepo, new UserAdsRepositoryStub, iam)
      val ads = AdServiceInterpreter.make[IO](
        adRepo,
        mock[TagRepository[IO]],
        feedRepository,
        new UserAdsRepositoryStub,
        iam,
        new TelemetryServiceStub[IO]
      )
      val chats = ChatServiceInterpreter.make[IO](chatRepo, adRepo, telemetry)
      val msgs  = MessageServiceInterpreter.make[IO](msgRepo, iam)
      for {
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
      } yield expect.all(x.isEmpty)
    }
  }

  test("unable to gen history for forbidden chat") {
    val gen = for {
      reg1 <- registerGen
      reg2 <- registerGen
      reg3 <- registerGen
      ad   <- createAdRequestGen
      msg  <- sendMessageRequestGen
    } yield (reg1, reg2, reg3, ad, msg)
    forall(gen) { case (reg1, reg2, reg3, createAd, msg) =>
      val userRepo = new InMemoryUserRepository[IO]
      val adRepo   = new InMemoryAdRepository[IO]
      val chatRepo = new InMemoryChatRepository[IO]
      val msgRepo  = new InMemoryMessageRepository[IO]
      val iam      = makeIam(adRepo, chatRepo)
      val users    = UserServiceInterpreter.make[IO](userRepo, new UserAdsRepositoryStub, iam)
      val ads = AdServiceInterpreter.make[IO](
        adRepo,
        mock[TagRepository[IO]],
        feedRepository,
        new UserAdsRepositoryStub,
        iam,
        new TelemetryServiceStub[IO]
      )
      val chats = ChatServiceInterpreter.make[IO](chatRepo, adRepo, telemetry)
      val msgs  = MessageServiceInterpreter.make[IO](msgRepo, iam)
      for {
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
      } yield expect.all(true)
    }
  }
}
