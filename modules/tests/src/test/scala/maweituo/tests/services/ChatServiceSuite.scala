package maweituo.tests.services

import maweituo.domain.ads.services.{ AdService, ChatService }
import maweituo.domain.errors.{CannotCreateChatWithMyself, ChatAlreadyExists, InvalidChatId}
import maweituo.domain.services.*
import maweituo.domain.users.services.*
import maweituo.interpreters.*
import maweituo.interpreters.ads.{ AdServiceInterpreter, ChatServiceInterpreter }
import maweituo.interpreters.users.UserServiceInterpreter
import maweituo.tests.generators.{chatIdGen, createAdRequestGen, registerGen}
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.*
import maweituo.tests.services.stubs.*

import cats.effect.IO
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object ChatServiceSuite extends SimpleIOSuite with Checkers:

  given Logger[IO]           = NoOpLogger[IO]
  given TelemetryService[IO] = new TelemetryServiceStub[IO]

  private def makeTestServices: (UserService[F], AdService[F], ChatService[F]) =
    val telemetry        = new TelemetryServiceStub
    val userRepo         = InMemoryRepositoryFactory.users
    val adRepo           = InMemoryRepositoryFactory.ads
    val chatRepo         = InMemoryRepositoryFactory.chats
    given IAMService[IO] = makeIAMService(adRepo, chatRepo)
    val users            = UserServiceInterpreter.make(userRepo)
    val feedRepository   = RepositoryStubFactory.feed
    val ads              = AdServiceInterpreter.make(adRepo, feedRepository)
    val chats            = ChatServiceInterpreter.make(chatRepo, adRepo)
    (users, ads, chats)

  test("invalid chat id") {
    val (users, ads, chats) = makeTestServices
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

  test("create works") {
    val (users, ads, chats) = makeTestServices
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

  test("can't create chat with myself") {
    val (users, ads, chats) = makeTestServices
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
    val (users, ads, chats) = makeTestServices
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
