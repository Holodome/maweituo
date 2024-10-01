package maweituo.tests.services

import java.time.Instant

import maweituo.domain.ads.services.{AdService, ChatService, MessageService}
import maweituo.domain.errors.ChatAccessForbidden
import maweituo.domain.services.*
import maweituo.domain.users.services.*
import maweituo.effects.TimeSource
import maweituo.interp.*
import maweituo.interp.ads.{AdServiceInterp, ChatServiceInterp, MessageServiceInterp}
import maweituo.interp.users.UserServiceInterp
import maweituo.tests.generators.{createAdRequestGen, registerGen, sendMessageRequestGen}
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.InMemoryRepoFactory
import maweituo.tests.services.stubs.TelemetryServiceStub

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers
import maweituo.domain.ads.repos.MessageRepo
import scala.util.control.NoStackTrace
import maweituo.domain.ads.messages.ChatId
import maweituo.tests.repos.inmemory.InMemoryMessageRepo
import maweituo.domain.ads.messages.Message

object MessageServiceSuite extends SimpleIOSuite with Checkers:

  given Logger[IO]           = NoOpLogger[IO]
  given TelemetryService[IO] = new TelemetryServiceStub[IO]

  private val epoch: Long = 1711564995
  private given timeSource: TimeSource[IO] = new:
    def instant: IO[Instant] = Instant.ofEpochSecond(epoch).pure[IO]

  private def makeTestServices(msgRepo: MessageRepo[IO] = InMemoryRepoFactory.msgs)
      : (UserService[IO], AdService[IO], ChatService[IO], MessageService[IO]) =
    val telemetry             = new TelemetryServiceStub
    val userRepo              = InMemoryRepoFactory.users
    val adRepo                = InMemoryRepoFactory.ads
    val chatRepo              = InMemoryRepoFactory.chats
    given iam: IAMService[IO] = makeIAMService(adRepo, chatRepo)
    val users                 = UserServiceInterp.make(userRepo)
    val feedRepo        = RepoStubFactory.feed
    val ads                   = AdServiceInterp.make(adRepo, feedRepo)
    val chats                 = ChatServiceInterp.make(chatRepo, adRepo)
    val msgs                  = MessageServiceInterp.make(msgRepo)(using MonadThrow[IO], timeSource, iam)
    (users, ads, chats, msgs)

  test("history internal error") {
    case class TestError() extends NoStackTrace
    class ChatRepo extends InMemoryMessageRepo[IO]:
      override def chatHistory(chatId: ChatId) = IO.raiseError(TestError())
    val (users, ads, chats, msgs) = makeTestServices(new ChatRepo)
    val gen =
      for
        reg      <- registerGen
        otherReg <- registerGen
        ad       <- createAdRequestGen
      yield (reg, otherReg, ad)
    forall(gen) { case (reg, otherReg, createAd) =>
      for
        u1   <- users.create(reg)
        u2   <- users.create(otherReg)
        ad   <- ads.create(u1, createAd)
        chat <- chats.create(ad, u2)
        x    <- msgs.history(chat, u2).attempt
      yield expect.same(Left(TestError()), x)
    }
  }

  test("empty chat is empty") {
    val (users, ads, chats, msgs) = makeTestServices()
    val gen =
      for
        reg      <- registerGen
        otherReg <- registerGen
        ad       <- createAdRequestGen
      yield (reg, otherReg, ad)
    forall(gen) { case (reg, otherReg, createAd) =>
      for
        u1      <- users.create(reg)
        u2      <- users.create(otherReg)
        ad      <- ads.create(u1, createAd)
        chat    <- chats.create(ad, u2)
        history <- msgs.history(chat, u2)
      yield expect.all(history.messages.isEmpty)
    }
  }

  test("basic message works") {
    val (users, ads, chats, msgs) = makeTestServices()
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
        NonEmptyList.of(
          expect.same(m.text, msg.text),
          expect.same(m.chat, chat),
          expect.same(m.at.getEpochSecond, epoch),
          expect.same(m.sender, u2)
        ).reduce
      }
    }
  }

  test("send internal error") {
    case class TestError() extends NoStackTrace
    class ChatRepo extends InMemoryMessageRepo[IO]:
      override def send(message: Message) = IO.raiseError(TestError())
    val (users, ads, chats, msgs) = makeTestServices(new ChatRepo)
    val gen =
      for
        reg      <- registerGen
        otherReg <- registerGen
        ad       <- createAdRequestGen
        msg      <- sendMessageRequestGen
      yield (reg, otherReg, ad, msg)
    forall(gen) { case (reg, otherReg, createAd, msg) =>
      for
        u1   <- users.create(reg)
        u2   <- users.create(otherReg)
        ad   <- ads.create(u1, createAd)
        chat <- chats.create(ad, u2)
        x    <- msgs.send(chat, u2, msg).attempt
      yield expect.same(Left(TestError()), x)
    }
  }

  test("unable to send to forbidden chat") {
    val (users, ads, chats, msgs) = makeTestServices()
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
        x    <- msgs.send(chat, u3, msg).attempt
      yield expect.same(Left(ChatAccessForbidden(chat)), x)
    }
  }

  test("unable to gen history for forbidden chat") {
    val (users, ads, chats, msgs) = makeTestServices()
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
        x    <- msgs.history(chat, u3).attempt
      yield expect.same(Left(ChatAccessForbidden(chat)), x)
    }
  }
