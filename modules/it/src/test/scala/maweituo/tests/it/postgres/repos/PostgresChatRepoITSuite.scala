package maweituo
package tests
package it
package postgres
package repos

import maweituo.domain.all.*

import weaver.GlobalRead

class PostgresChatRepoITSuite(global: GlobalRead) extends PostgresITSuite(global):

  private def chatTest(name: String)(fn: (UserRepo[IO], AdRepo[IO], ChatRepo[IO]) => F[Expectations]) =
    pgTest(name) { postgres =>
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
