package com.holodome.services

import cats.MonadThrow
import com.holodome.domain.users._
import com.holodome.repositories.JwtRepository
import dev.profunktor.auth.jwt.JwtToken
import cats.syntax.all._
import com.holodome.auth.{JwtTokens, PasswordHashing}

trait AuthService[F[_]] {
  def login(username: Username, password: Password): F[JwtToken]
  def logout(username: Username): F[Unit]
}

object AuthService {
  def make[F[_]: MonadThrow](
      userService: UserService[F],
      jwtRepo: JwtRepository[F],
      tokens: JwtTokens[F]
  ): AuthService[F] = new AuthServiceInterpreter(userService, jwtRepo, tokens)

  private final class AuthServiceInterpreter[F[_]: MonadThrow](
      userService: UserService[F],
      jwtRepo: JwtRepository[F],
      tokens: JwtTokens[F]
  ) extends AuthService[F] {
    override def login(username: Username, password: Password): F[JwtToken] =
      userService
        .find(username)
        .getOrElseF(NoUserFound(username).raiseError[F, User])
        .flatMap { user =>
          if (passwordsMatch(user, password)) {
            jwtRepo
              .getToken(username)
              .getOrElseF(tokens.create flatMap { t =>
                jwtRepo.storeToken(username, t).map(_ => t)
              })
          } else {
            InvalidPassword(username).raiseError[F, JwtToken]
          }
        }

    override def logout(username: Username): F[Unit] =
      jwtRepo.deleteToken(username)
  }

  private def passwordsMatch(user: User, str: Password): Boolean =
    user.hashedPassword == PasswordHashing.hashSaltPassword(str, user.salt)
}
