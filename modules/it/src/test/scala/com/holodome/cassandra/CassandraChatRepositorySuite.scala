package com.holodome.cassandra

import cats.syntax.all._
import cats.effect.IO
import cats.Show
import com.holodome.domain.errors.InvalidChatId
import com.holodome.domain.messages.Chat
import com.holodome.generators.{adIdGen, chatIdGen, userIdGen}
import com.holodome.repositories.cassandra.CassandraChatRepository

object CassandraChatRepositorySuite extends CassandraSuite {
  implicit val chatShow: Show[Chat] = Show.show(_ => "Chat")
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
        c <- repo.find(chat.id).getOrRaise(InvalidChatId())
      } yield expect.all(
        c.id === chat.id,
        c.client === chat.client,
        c.adAuthor === chat.adAuthor,
        c.adId === chat.adId
      )
    }
  }
}
