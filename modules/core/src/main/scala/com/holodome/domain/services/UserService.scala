package com.holodome.domain.services

import com.holodome.domain.ads.AdId
import com.holodome.domain.users.*

trait UserService[F[_]]:
  def create(body: RegisterRequest): F[UserId]
  def get(id: UserId): F[User]
  def delete(subject: UserId, authId: UserId): F[Unit]
  def update(update: UpdateUserRequest, authId: UserId): F[Unit]
  def getAds(userId: UserId): F[List[AdId]]
