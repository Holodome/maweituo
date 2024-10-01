package maweituo.tests.services

import scala.util.control.NoStackTrace

import cats.data.OptionT
import cats.effect.IO

import maweituo.domain.ads.messages.{Chat, ChatId}
import maweituo.domain.ads.repos.ChatRepo
import maweituo.domain.ads.services.{AdService, ChatService}
import maweituo.domain.errors.{CannotCreateChatWithMyself, ChatAlreadyExists, InvalidChatId}
import maweituo.domain.services.*
import maweituo.domain.users.services.*
import maweituo.interp.*
import maweituo.interp.ads.{AdServiceInterp, ChatServiceInterp}
import maweituo.interp.users.UserServiceInterp
import maweituo.tests.generators.{chatIdGen, createAdRequestGen, registerGen}
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.*
import maweituo.tests.services.stubs.*

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object ChatServiceSuite extends SimpleIOSuite with Checkers:

  given Logger[IO]           = NoOpLogger[IO]
  given TelemetryService[IO] = new TelemetryServiceStub[IO]

  private def makeTestServices(chatRepo: ChatRepo[IO] = InMemoryRepoFactory.chats)
      : (UserService[F], AdService[F], ChatService[F]) =
    val telemetry        = new TelemetryServiceStub
    val userRepo         = InMemoryRepoFactory.users
    val adRepo           = InMemoryRepoFactory.ads
    given IAMService[IO] = makeIAMService(adRepo, chatRepo)
    val users            = UserServiceInterp.make(userRepo)
    val feedRepo   = RepoStubFactory.feed
    val ads              = AdServiceInterp.make(adRepo, feedRepo)
    val chats            = ChatServiceInterp.make(chatRepo, adRepo)
    (users, ads, chats)

  test("invalid chat id") {
    val (users, ads, chats) = makeTestServices()
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
  }

  test("get internal error") {
    case class TestError() extends NoStackTrace
    class ChatRepo extends InMemoryChatRepo[IO]:
      override def find(chatId: ChatId) = OptionT(IO.raiseError(TestError()))
    val (users, ads, chats) = makeTestServices(new ChatRepo)
    val gen =
      for
        u <- registerGen
        c <- chatIdGen
      yield u -> c
    forall(gen) { (reg, chat) =>
      for
        user <- users.create(reg)
        x    <- chats.get(chat, user).attempt
      yield expect.same(Left(TestError()), x)
    }
  }

  test("create works") {
    val (users, ads, chats) = makeTestServices()
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
  }

  test("create internal error") {
    case class TestError() extends NoStackTrace
    class ChatRepo extends InMemoryChatRepo[IO]:
      override def create(chat: Chat): F[Unit] = IO.raiseError(TestError())
    val (users, ads, chats) = makeTestServices(new ChatRepo)
    val gen =
      for
        reg      <- registerGen
        otherReg <- registerGen
        ad       <- createAdRequestGen
      yield (reg, otherReg, ad)
    forall(gen) { (reg, otherReg, createAd) =>
      for
        u1 <- users.create(reg)
        u2 <- users.create(otherReg)
        ad <- ads.create(u1, createAd)
        x  <- chats.create(ad, u2).attempt
      yield expect.same(Left(TestError()), x)
    }
  }

  test("can't create chat with myself") {
    val (users, ads, chats) = makeTestServices()
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
  }

  test("can't create same chat multiple times") {
    val (users, ads, chats) = makeTestServices()
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
  }
