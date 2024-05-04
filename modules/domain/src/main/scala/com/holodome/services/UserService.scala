package com.holodome.services

import com.holodome.domain.users._

trait UserService[F[_]] {
  def create(body: RegisterRequest): F[UserId]
  def get(id: UserId): F[User]
  def delete(subject: UserId, authorized: UserId): F[Unit]
  def update(update: UpdateUserRequest, authorized: UserId): F[Unit]
}