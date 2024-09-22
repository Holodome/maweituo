package com.holodome.modules

import com.holodome.domain.repositories.*

sealed abstract class Repositories[F[_]]:
  val users: UserRepository[F]
  val userAds: UserAdsRepository[F]
  val ads: AdvertisementRepository[F]
  val tags: TagRepository[F]
  val chats: ChatRepository[F]
  val messages: MessageRepository[F]
  val images: AdImageRepository[F]
  val feed: FeedRepository[F]

object Repositories:
  def makePostgres[F[_]]: Repositories[F] = ???
