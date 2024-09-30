package com.holodome.tests.repositories.inmemory

import com.holodome.domain.messages.*
import com.holodome.tests.generators.*
import com.holodome.tests.repositories.*

import cats.effect.IO
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object InMemoryMessageRepositorySuite extends SimpleIOSuite with Checkers:

  private def repo = InMemoryRepositoryFactory.msgs[IO]

  val msgGen =
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
