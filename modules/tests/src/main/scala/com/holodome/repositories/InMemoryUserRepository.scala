package com.holodome.repositories

import cats.data.OptionT
import com.holodome.domain.users
import com.holodome.domain.users.{User, UserId}

import scala.collection.concurrent.TrieMap
import cats.syntax.all._
import cats.effect.Sync

final class InMemoryUserRepository[F[_]: Sync] extends UserRepository[F] {

  private val map = new TrieMap[UserId, User]

  override def create(request: users.User): F[Unit] =
    Sync[F].delay { map.addOne(request.id -> request) }

  override def all(): F[List[users.User]] =
    Sync[F].delay { map.values.toList }

  override def find(userId: users.UserId): OptionT[F, users.User] =
    OptionT(Sync[F].delay { map.get(userId) })

  override def findByEmail(email: users.Email): OptionT[F, users.User] =
    OptionT(Sync[F].delay { map.values.find(_.email === email) })

  override def findByName(name: users.Username): OptionT[F, users.User] =
    OptionT(Sync[F].delay { map.values.find(_.name === name) })

  override def delete(id: users.UserId): F[Unit] =
    Sync[F].delay { map.remove(id) }

  override def update(update: users.UpdateUserInternal): F[Unit] =
    Sync[F].delay {
      map.get(update.id).map { value =>
        val newUser = User(
          update.id,
          update.name.getOrElse(value.name),
          update.email.getOrElse(value.email),
          update.password.getOrElse(value.hashedPassword),
          value.salt,
          value.ads
        )
        map.addOne(value.id -> newUser)
      }
    }
}
