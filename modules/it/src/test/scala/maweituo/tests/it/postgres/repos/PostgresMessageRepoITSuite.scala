package maweituo
package tests
package it
package postgres
package repos

import maweituo.domain.all.*
import maweituo.postgres.repos.all.*
import maweituo.tests.resources.*

import doobie.util.transactor.Transactor
import weaver.GlobalRead

class PostgresMessageRepoITSuite(global: GlobalRead) extends ResourceSuite:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] =
    global.postgres

  private def msgTest(name: String)(fn: (UserRepo[IO], AdRepo[IO], ChatRepo[IO], MessageRepo[IO]) => F[Expectations]) =
    itTest(name) { postgres =>
      val users = PostgresUserRepo.make(postgres)
      val ads   = PostgresAdRepo.make(postgres)
      val chats = PostgresChatRepo.make(postgres)
      val msgs  = PostgresMessageRepo.make(postgres)
      fn(users, ads, chats, msgs)
    }

  private def msgGen(chatId: ChatId, sender: UserId) =
    for
      text <- msgTextGen
      at   <- instantGen
    yield Message(sender, chatId, text, at)

  private val gen =
    for
      u   <- userGen
      u1  <- userGen
      ad0 <- adGen
      ad = ad0.copy(authorId = u.id)
      chatId <- chatIdGen
      chat = Chat(chatId, adId = ad.id, u.id, u1.id)
      msg <- msgGen(chatId, u.id)
    yield (u, u1, ad, chat, msg)

  msgTest("send and get history") { (users, ads, chats, msgs) =>
    forall(gen) { (u, u1, ad, chat, msg) =>
      for
        _ <- users.create(u)
        _ <- users.create(u1)
        _ <- ads.create(ad)
        _ <- chats.create(chat)
        _ <- msgs.send(msg)
        x <- msgs.chatHistory(chat.id, Pagination(0)).map(_.items)
      yield expect.same(List(msg), x.map(_.copy(at = msg.at)))
    }
  }
