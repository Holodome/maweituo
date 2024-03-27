package com.holodome.utils.repositories

import cats.data.OptionT
import com.holodome.domain.users
import com.holodome.domain.users.{User, UserId}

import scala.collection.concurrent.TrieMap
import cats.syntax.all._
import cats.Applicative
import com.holodome.repositories.UserRepository

final class InMemoryUserRepository[F[_]: Applicative] extends UserRepository[F] {

  private val map = new TrieMap[UserId, User]

  override def create(request: users.User): F[Unit] =
    Applicative[F].pure { map.addOne(request.id -> request) }

  override def all(): F[List[users.User]] =
    Applicative[F].pure { map.values.toList }

  override def find(userId: users.UserId): OptionT[F, users.User] =
    OptionT(Applicative[F].pure { map.get(userId) })

  override def findByEmail(email: users.Email): OptionT[F, users.User] =
    OptionT(Applicative[F].pure { map.values.find(_.email === email) })

  override def findByName(name: users.Username): OptionT[F, users.User] =
    OptionT(Applicative[F].pure { map.values.find(_.name === name) })

  override def delete(id: users.UserId): F[Unit] =
    Applicative[F].pure { map.remove(id) }

  override def update(update: users.UpdateUserInternal): F[Unit] =
    Applicative[F].pure {
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
