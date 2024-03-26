package com.holodome.repositories

import cats.data.OptionT
import com.holodome.domain.users
import com.holodome.domain.users.{User, UserId}

import scala.collection.concurrent.TrieMap
import cats.syntax.all._
import cats.Applicative

final class InMemoryUserRepository[F[_]: Applicative] extends UserRepository[F] {

  private val map = new TrieMap[UserId, User]

  override def create(request: users.User): F[Unit] =
    map.addOne(request.id -> request).pure[F].map(_ => ())

  override def all(): F[List[users.User]] =
    map.values.toList.pure[F]

  override def find(userId: users.UserId): OptionT[F, users.User] =
    OptionT(map.get(userId).pure[F])

  override def findByEmail(email: users.Email): OptionT[F, users.User] =
    OptionT(map.values.find(_.email === email).pure[F])

  override def findByName(name: users.Username): OptionT[F, users.User] =
    OptionT(map.values.find(_.name === name).pure[F])

  override def delete(id: users.UserId): F[Unit] =
    map.remove(id).pure[F].map(_ => ())

  override def update(update: users.UpdateUserInternal): F[Unit] = {
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
    Applicative[F].unit
  }
}
