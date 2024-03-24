package com.holodome.services

import cats.MonadThrow
import cats.data.OptionT
import com.holodome.domain.users._
import com.holodome.repositories.{AuthedUserRepository, JwtRepository}
import dev.profunktor.auth.jwt.JwtToken
import cats.syntax.all._
import com.holodome.auth.{JwtTokens, PasswordHashing}

trait AuthService[F[_]] {
  def login(username: Username, password: Password): F[JwtToken]
  def logout(username: Username, token: JwtToken): F[Unit]
  def authed(token: JwtToken): OptionT[F, AuthedUser]
}

object AuthService {
  def make[F[_]: MonadThrow](
      userService: UserService[F],
      jwtRepo: JwtRepository[F],
      authedUserRepo: AuthedUserRepository[F],
      tokens: JwtTokens[F]
  ): AuthService[F] = new AuthServiceInterpreter(userService, jwtRepo, authedUserRepo, tokens)

  private final class AuthServiceInterpreter[F[_]: MonadThrow](
      userService: UserService[F],
      jwtRepo: JwtRepository[F],
      authedUserRepo: AuthedUserRepository[F],
      tokens: JwtTokens[F]
  ) extends AuthService[F] {
    override def login(username: Username, password: Password): F[JwtToken] =
      userService
        .find(username)
        .getOrElseF(NoUserFound(username).raiseError[F, User])
        .flatMap { user =>
          if (passwordsMatch(user, password)) {
            jwtRepo
              .get(username)
              .getOrElseF(tokens.create flatMap { t =>
                jwtRepo.store(username, t) *>
                  authedUserRepo.store(t, username).map(_ => t)
              })
          } else {
            InvalidPassword(username).raiseError[F, JwtToken]
          }
        }

    override def logout(username: Username, token: JwtToken): F[Unit] =
      jwtRepo.delete(username) *> authedUserRepo.delete(token)

    override def authed(token: JwtToken): OptionT[F, AuthedUser] =
      authedUserRepo.get(token).map(AuthedUser.apply)
  }

  private def passwordsMatch(user: User, str: Password): Boolean =
    user.hashedPassword == PasswordHashing.hashSaltPassword(str, user.salt)
}
