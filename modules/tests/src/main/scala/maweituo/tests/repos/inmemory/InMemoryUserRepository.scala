package maweituo
package tests
package repos
package inmemory

import scala.collection.concurrent.TrieMap

class InMemoryUserRepo[F[_]: Sync] extends UserRepo[F]:

  private val map = new TrieMap[UserId, User]

  override def create(request: User): F[Unit] =
    Sync[F] delay map.addOne(request.id -> request)

  override def all: F[List[User]] =
    Sync[F] delay map.values.toList

  override def find(userId: UserId): OptionT[F, User] =
    OptionT(Sync[F] delay map.get(userId))

  override def findByEmail(email: Email): OptionT[F, User] =
    OptionT(Sync[F] delay map.values.find(_.email === email))

  override def findByName(name: Username): OptionT[F, User] =
    OptionT(Sync[F] delay map.values.find(_.name === name))

  override def delete(id: UserId): F[Unit] =
    Sync[F] delay map.remove(id)

  override def update(update: UpdateUserRepoRequest): F[Unit] =
    Sync[F] delay map.get(update.id).map { value =>
      val newUser = User(
        update.id,
        update.name.getOrElse(value.name),
        update.email.getOrElse(value.email),
        update.password.getOrElse(value.hashedPassword),
        value.salt,
        value.createdAt,
        update.at
      )
      map.addOne(value.id -> newUser)
    }
