package com.holodome.repositories.cassandra

import cats.syntax.all._
import cats.data.OptionT
import cats.effect.Async
import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.holodome.domain.messages._
import com.holodome.domain.users.UserId
import com.holodome.domain.ads.AdId
import com.holodome.repositories.ChatRepository
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext
import com.holodome.cql.codecs._

object CassandraChatRepository {
  def make[F[_]: Async](session: CassandraSession[F]): ChatRepository[F] =
    new CassandraChatRepository(session)
}

sealed class CassandraChatRepository[F[_]: Async] private (session: CassandraSession[F])
    extends ChatRepository[F] {

  override def create(chat: Chat): F[Unit] =
    createQuery(chat).execute(session).void

  override def find(chatId: ChatId): OptionT[F, Chat] =
    OptionT(findQuery(chatId).select(session).head.compile.last)

  override def findByAdAndClient(adId: AdId, client: UserId): OptionT[F, ChatId] =
    OptionT(findByAdAndClientQuery(adId, client).select(session).head.compile.last)

  private def createQuery(chat: Chat) =
    cql"insert into local.chats (id, ad_id, ad_author_id, client_id) values (${chat.id.id}, ${chat.adId.value}, ${chat.adAuthor.value}, ${chat.client})"
      .config(
        _.setConsistencyLevel(ConsistencyLevel.QUORUM)
      )

  private def findQuery(chatId: ChatId) =
    cql"select id, ad_id, ad_author_id, client_id from local.chats where id = ${chatId.id}".as[Chat]

  private def findByAdAndClientQuery(adId: AdId, client: UserId) =
    cql"select id from local.chats where ad_id = ${adId.value} and client_id = ${client.value} allow filtering"
      .as[ChatId]
}
