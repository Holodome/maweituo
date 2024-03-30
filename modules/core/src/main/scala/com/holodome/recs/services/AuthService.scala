package com.holodome.services

import cats.MonadThrow
import cats.data.OptionT
import cats.syntax.all._
import com.holodome.auth.{JwtTokens, PasswordHashing}
import com.holodome.domain.users._
import com.holodome.infrastructure.EphemeralDict
import dev.profunktor.auth.jwt.JwtToken

trait AuthService[F[_]] {
  def login(username: Username, password: Password): F[JwtToken]
  def logout(uid: UserId, token: JwtToken): F[Unit]
  def authed(token: JwtToken): OptionT[F, AuthedUser]
}

object AuthService {
  def make[F[_]: MonadThrow](
      userService: UserService[F],
      jwtDict: EphemeralDict[F, UserId, JwtToken],
      authedUserDict: EphemeralDict[F, JwtToken, UserId],
      tokens: JwtTokens[F]
  ): AuthService[F] = new AuthServiceInterpreter(userService, jwtDict, authedUserDict, tokens)

  private final class AuthServiceInterpreter[F[_]: MonadThrow](
      userService: UserService[F],
      jwtDict: EphemeralDict[F, UserId, JwtToken],
      authedUserDict: EphemeralDict[F, JwtToken, UserId],
      tokens: JwtTokens[F]
  ) extends AuthService[F] {
    override def login(username: Username, password: Password): F[JwtToken] =
      userService
        .findByName(username)
        .flatMap { user =>
          if (passwordsMatch(user, password)) {
            jwtDict
              .get(user.id)
              .getOrElseF(tokens.create flatMap { t =>
                jwtDict.store(user.id, t) *>
                  authedUserDict.store(t, user.id).map(_ => t)
              })
          } else {
            InvalidPassword(username).raiseError[F, JwtToken]
          }
        }

    override def logout(uid: UserId, token: JwtToken): F[Unit] =
      jwtDict.delete(uid) *> authedUserDict.delete(token)

    override def authed(token: JwtToken): OptionT[F, AuthedUser] =
      authedUserDict.get(token).map(AuthedUser.apply)
  }

  private def passwordsMatch(user: User, str: Password): Boolean =
    user.hashedPassword == PasswordHashing.hashSaltPassword(str, user.salt)
}
