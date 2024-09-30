package com.holodome.tests.repos.inmemory

import com.holodome.domain.messages.Chat
import com.holodome.tests.generators.*
import com.holodome.tests.repos.*

import cats.effect.IO
import org.scalacheck.Gen
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object InMemoryChatRepositorySuite extends SimpleIOSuite with Checkers:

  private val chatGen: Gen[Chat] =
    for
      id <- chatIdGen
      ad <- adIdGen
      u1 <- userIdGen
      u2 <- userIdGen
    yield Chat(id, ad, u1, u2)

  private def repo = InMemoryRepositoryFactory.chats[IO]

  test("create and find") {
    val chats = repo
    forall(chatGen) { chat =>
      for
        _ <- chats.create(chat)
        u <- chats.find(chat.id).value
      yield expect.same(u, Some(chat))
    }
  }

  test("create and find by client") {
    val chats = repo
    forall(chatGen) { chat =>
      for
        _ <- chats.create(chat)
        u <- chats.findByAdAndClient(chat.adId, chat.client).value
      yield expect.same(u, Some(chat))
    }
  }
