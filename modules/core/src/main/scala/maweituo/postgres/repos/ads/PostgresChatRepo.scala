package maweituo
package postgres
package repos
package ads

import maweituo.domain.all.*

import doobie.*
import doobie.implicits.*
export doobie.implicits.given
import doobie.Transactor

import cats.syntax.all.*
import cats.data.OptionT
import cats.effect.Async
object PostgresChatRepo:
  def make[F[_]: Async](xa: Transactor[F]): ChatRepo[F] = new:

    def create(chat: Chat): F[Unit] =
      sql"""
        insert into chats(id, ad_id, ad_author_id, client_id) 
        values (${chat.id}::uuid, ${chat.adId}::uuid, ${chat.adAuthor}::uuid, ${chat.client}::uuid)
      """.update.run.transact(xa).void

    def find(chatId: ChatId): OptionT[F, Chat] =
      OptionT(
        sql"select id, ad_id, ad_author_id, client_id from chats where id = $chatId::uuid"
          .query[Chat].option.transact(xa)
      )

    def findByAdAndClient(adId: AdId, client: UserId): OptionT[F, Chat] =
      OptionT(
        sql"""
          select id, ad_id, ad_author_id, client_id from chats 
          where ad_id = $adId::uuid and client_id = $client::uuid
        """.query[Chat].option.transact(xa)
      )

    def findForAd(ad: AdId): F[List[Chat]] =
      sql"""
        select id, ad_id, ad_author_id, client_id from chats 
        where ad_id = $ad::uuid
      """.query[Chat].to[List].transact(xa)
