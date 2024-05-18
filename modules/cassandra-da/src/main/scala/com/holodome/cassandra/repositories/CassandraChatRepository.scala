package com.holodome.cassandra.repositories

import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all._
import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.holodome.cassandra.cql.codecs._
import com.holodome.domain.ads.AdId
import com.holodome.domain.messages._
import com.holodome.domain.repositories.ChatRepository
import com.holodome.domain.users.UserId
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext

object CassandraChatRepository {
  def make[F[_]: Async](session: CassandraSession[F]): ChatRepository[F] =
    new CassandraChatRepository(session)
}

private final class CassandraChatRepository[F[_]: Async](session: CassandraSession[F])
    extends ChatRepository[F] {

  override def create(chat: Chat): F[Unit] =
    cql"insert into local.chats (id, ad_id, ad_author_id, client_id) values (${chat.id.id}, ${chat.adId.value}, ${chat.adAuthor.value}, ${chat.client})"
      .config(
        _.setConsistencyLevel(ConsistencyLevel.QUORUM)
      )
      .execute(session)
      .void

  override def find(chatId: ChatId): OptionT[F, Chat] =
    OptionT(
      cql"select id, ad_id, ad_author_id, client_id from local.chats where id = ${chatId.id}"
        .as[Chat]
        .select(session)
        .head
        .compile
        .last
    )

  override def findByAdAndClient(adId: AdId, client: UserId): OptionT[F, ChatId] =
    OptionT(
      cql"select id from local.chats where ad_id = ${adId.value} and client_id = ${client.value} allow filtering"
        .as[ChatId]
        .select(session)
        .head
        .compile
        .last
    )

}
