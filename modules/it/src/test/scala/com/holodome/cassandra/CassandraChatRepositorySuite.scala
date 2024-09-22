package com.holodome.cassandra

import cats.Show
import cats.effect.IO
import cats.syntax.all._
import com.holodome.cassandra.repositories.CassandraChatRepository
import com.holodome.domain.errors.InvalidChatId
import com.holodome.domain.messages.Chat
import com.holodome.tests.generators.{adIdGen, chatIdGen, userIdGen}

object CassandraChatRepositorySuite extends CassandraSuite {
  given Show[Chat] = Show.show(_ => "Chat")
  test("basic operations work") { cassandra =>
    val gen = for {
      id       <- chatIdGen
      adId     <- adIdGen
      adAuthor <- userIdGen
      client   <- userIdGen
    } yield Chat(id, adId, adAuthor, client)
    forall(gen) { chat =>
      val repo = CassandraChatRepository.make[IO](cassandra)
      for {
        _ <- repo.create(chat)
        c <- repo.find(chat.id).getOrRaise(InvalidChatId(chat.id))
      } yield expect.all(
        c.id === chat.id,
        c.client === chat.client,
        c.adAuthor === chat.adAuthor,
        c.adId === chat.adId
      )
    }
  }
}
