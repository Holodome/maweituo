package maweituo
package domain
package repos
package users

import maweituo.domain.users.{Email, UpdateUserRepoRequest, User, UserId, Username}

trait UserRepo[F[_]]:
  def create(request: User): F[Unit]
  def all: F[List[User]]
  def find(userId: UserId): OptionT[F, User]
  def findByEmail(email: Email): OptionT[F, User]
  def findByName(name: Username): OptionT[F, User]
  def delete(id: UserId): F[Unit]
  def update(update: UpdateUserRepoRequest): F[Unit]

object UserRepo:
  import maweituo.logic.errors.*

  extension [F[_]: MonadThrow](repo: UserRepo[F])
    def get(userId: UserId): F[User] =
      repo.find(userId).getOrRaise(DomainError.InvalidUserId(userId))

    def getByName(name: Username): F[User] =
      repo.findByName(name).getOrRaise(DomainError.NoUserWithName(name))

    def getByEmail(email: Email): F[User] =
      repo.findByEmail(email).getOrRaise(DomainError.NoUserWithEmail(email))
