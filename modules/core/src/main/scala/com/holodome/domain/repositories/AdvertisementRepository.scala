package com.holodome.domain.repositories

import com.holodome.domain.ads.*
import com.holodome.domain.errors.InvalidAdId
import com.holodome.domain.images.ImageId
import com.holodome.domain.messages.ChatId

import cats.MonadThrow
import cats.data.OptionT

trait AdvertisementRepository[F[_]]:
  def create(ad: Advertisement): F[Unit]
  def all: F[List[Advertisement]]
  def find(id: AdId): OptionT[F, Advertisement]
  def delete(id: AdId): F[Unit]
  def addTag(id: AdId, tag: AdTag): F[Unit]
  def addImage(id: AdId, image: ImageId): F[Unit]
  def removeTag(id: AdId, tag: AdTag): F[Unit]
  def removeImage(id: AdId, image: ImageId): F[Unit]
  def addChat(id: AdId, chatId: ChatId): F[Unit]
  def markAsResolved(id: AdId): F[Unit]

object AdvertisementRepository:
  extension [F[_]: MonadThrow](repo: AdvertisementRepository[F])
    def get(id: AdId): F[Advertisement] =
      repo.find(id).getOrRaise(InvalidAdId(id))
