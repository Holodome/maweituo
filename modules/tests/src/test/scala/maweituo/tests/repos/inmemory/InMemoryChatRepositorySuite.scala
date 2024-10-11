package maweituo
package tests
package repos
package inmemory

import maweituo.domain.all.*

object InMemoryChatRepoSuite extends SimpleIOSuite with Checkers:

  private val chatGen: Gen[Chat] =
    for
      id <- chatIdGen
      ad <- adIdGen
      u1 <- userIdGen
      u2 <- userIdGen
    yield Chat(id, ad, u1, u2)

  private def repo = InMemoryRepoFactory.chats[IO]

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
