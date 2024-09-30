package com.holodome.domain.users.services

import com.holodome.domain.users.*

trait UserService[F[_]]:
  def create(body: RegisterRequest): F[UserId]
  def get(id: UserId): F[User]
  def getByName(name: Username): F[User]
  def getByEmail(email: Email): F[User]
  def delete(subject: UserId, authId: UserId): F[Unit]
  def update(update: UpdateUserRequest, authId: UserId): F[Unit]
