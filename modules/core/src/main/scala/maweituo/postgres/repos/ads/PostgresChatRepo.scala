package maweituo.postgres.ads.repos

import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.given

import maweituo.domain.ads.AdId
import maweituo.domain.ads.messages.{Chat, ChatId}
import maweituo.domain.ads.repos.ChatRepo
import maweituo.domain.users.UserId
import maweituo.postgres.sql.codecs.given

import doobie.Transactor
import doobie.implicits.given

object PostgresChatRepo:
  def make[F[_]: Async](xa: Transactor[F]): ChatRepo[F] = new:
    def create(chat: Chat): F[Unit] =
      sql"""
        insert into chats(id, ad_id, ad_author_id, client_id) 
        values (${chat.id}, ${chat.adId}, ${chat.adAuthor}, ${chat.client})
      """.update.run.transact(xa).void

    def find(chatId: ChatId): OptionT[F, Chat] =
      OptionT(
        sql"select id, ad_id, ad_author_id, client_id from chats where id = $chatId".query[Chat].option.transact(xa)
      )

    def findByAdAndClient(adId: AdId, client: UserId): OptionT[F, Chat] =
      OptionT(
        sql"select id, ad_id, ad_author_id, client_id from chats where ad_id = $adId and client_id = $client".query[
          Chat
        ].option.transact(xa)
      )
