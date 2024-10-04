package maweituo.it.postgres.repos

import cats.effect.*

import maweituo.domain.ads.messages.Chat
import maweituo.domain.ads.repos.{AdRepo, ChatRepo}
import maweituo.domain.users.repos.UserRepo
import maweituo.postgres.ads.repos.{PostgresAdRepo, PostgresChatRepo}
import maweituo.postgres.repos.users.PostgresUserRepo
import maweituo.tests.ResourceSuite
import maweituo.tests.generators.{adGen, chatIdGen, userGen}
import maweituo.tests.resources.*
import maweituo.tests.utils.given

import doobie.util.transactor.Transactor
import weaver.*
import weaver.scalacheck.Checkers

class PostgresChatRepoITSuite(global: GlobalRead) extends ResourceSuite:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] =
    global.postgres

  private def chatTest(name: String)(fn: (UserRepo[IO], AdRepo[IO], ChatRepo[IO]) => F[Expectations]) =
    test(name) { postgres =>
      val users = PostgresUserRepo.make(postgres)
      val ads   = PostgresAdRepo.make(postgres)
      val chats = PostgresChatRepo.make(postgres)
      fn(users, ads, chats)
    }

  private val gen =
    for
      u   <- userGen
      u1  <- userGen
      ad0 <- adGen
      ad = ad0.copy(authorId = u.id)
      chatId <- chatIdGen
      chat = Chat(chatId, adId = ad.id, u.id, u1.id)
    yield (u, u1, ad, chat)

  chatTest("create and find") { (users, ads, chats) =>
    forall(gen) { (u, u1, ad, chat) =>
      for
        _ <- users.create(u)
        _ <- users.create(u1)
        _ <- ads.create(ad)
        _ <- chats.create(chat)
        x <- chats.find(chat.id).value
      yield expect.same(Some(chat), x)
    }
  }

  chatTest("find by ad and clinet") { (users, ads, chats) =>
    forall(gen) { (u, u1, ad, chat) =>
      for
        _ <- users.create(u)
        _ <- users.create(u1)
        _ <- ads.create(ad)
        _ <- chats.create(chat)
        x <- chats.findByAdAndClient(chat.adId, chat.client).value
      yield expect.same(Some(chat), x)
    }
  }
