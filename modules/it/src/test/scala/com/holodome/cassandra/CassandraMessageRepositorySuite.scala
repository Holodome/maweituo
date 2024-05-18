package com.holodome.cassandra

import cats.Show
import cats.effect.IO
import cats.syntax.all._
import com.holodome.cassandra.repositories.CassandraMessageRepository
import com.holodome.domain.messages.Message
import com.holodome.tests.generators._

object CassandraMessageRepositorySuite extends CassandraSuite {
  private implicit val showMessage: Show[Message] = Show.show(_ => "Message")
  test("basic operations work") { cassandra =>
    val gen = for {
      sender <- userIdGen
      chat   <- chatIdGen
      text   <- msgTextGen
      at     <- instantGen
    } yield Message(sender, chat, text, at)
    forall(gen) { msg =>
      val repo = CassandraMessageRepository.make[IO](cassandra)
      for {
        _ <- repo.send(msg)
        h <- repo.chatHistory(msg.chat)
        head = h.head
      } yield expect.all(head.chat === msg.chat, head.text === msg.text, head.sender === msg.sender)
    }
  }
}
