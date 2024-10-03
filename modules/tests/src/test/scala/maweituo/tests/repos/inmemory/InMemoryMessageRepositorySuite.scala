package maweituo.tests.repos.inmemory

import cats.effect.IO

import maweituo.domain.ads.messages.Message
import maweituo.tests.generators.*
import maweituo.tests.repos.*

import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object InMemoryMessageRepoSuite extends SimpleIOSuite with Checkers:

  private def repo = InMemoryRepoFactory.msgs[IO]

  private val msgGen =
    for
      chat <- chatIdGen
      text <- msgTextGen
      u1   <- userIdGen
      at   <- instantGen
    yield Message(u1, chat, text, at)

  test("empty history is empty") {
    val msgs = repo
    forall(chatIdGen) { chat =>
      for
        x <- msgs.chatHistory(chat)
      yield expect.same(List(), x)
    }
  }

  test("send message and get chat history") {
    val msgs = repo
    forall(msgGen) { msg =>
      for
        _ <- msgs.send(msg)
        x <- msgs.chatHistory(msg.chat)
      yield expect.same(List(msg), x)
    }
  }
