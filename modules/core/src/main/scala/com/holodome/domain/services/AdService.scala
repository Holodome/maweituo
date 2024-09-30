package com.holodome.domain.services

import com.holodome.domain.ads.*
import com.holodome.domain.users.UserId

trait AdService[F[_]]:
  def get(id: AdId): F[Advertisement]
  def create(authorId: UserId, create: CreateAdRequest): F[AdId]
  def delete(id: AdId, userId: UserId): F[Unit]
  def markAsResolved(id: AdId, userId: UserId, withWhom: UserId): F[Unit]
