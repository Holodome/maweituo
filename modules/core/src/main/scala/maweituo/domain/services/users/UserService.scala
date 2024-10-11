package maweituo
package domain
package services
package users

import maweituo.domain.users.*

trait UserService[F[_]]:
  def create(body: RegisterRequest): F[UserId]
  def get(id: UserId): F[User]
  def getByName(name: Username): F[User]
  def getByEmail(email: Email): F[User]
  def delete(subject: UserId)(using Identity): F[Unit]
  def update(update: UpdateUserRequest)(using Identity): F[Unit]
