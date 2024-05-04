package com.holodome.services

import cats.MonadThrow
import cats.data.OptionT
import cats.syntax.all._
import com.holodome.auth.JwtTokens
import com.holodome.auth.PasswordHashing
import com.holodome.domain.errors.InvalidPassword
import com.holodome.domain.users._
import com.holodome.infrastructure.EphemeralDict
import com.holodome.repositories.UserRepository
import dev.profunktor.auth.jwt.JwtToken
import org.typelevel.log4cats.Logger

trait AuthService[F[_]] {
  def login(username: Username, password: Password): F[JwtToken]
  def logout(uid: UserId, token: JwtToken): F[Unit]
  def authed(token: JwtToken): OptionT[F, AuthedUser]
}

object AuthService {
  def make[F[_]: MonadThrow: Logger](
      userRepo: UserRepository[F],
      jwtDict: EphemeralDict[F, UserId, JwtToken],
      authedUserDict: EphemeralDict[F, JwtToken, UserId],
      tokens: JwtTokens[F]
  ): AuthService[F] = new AuthServiceInterpreter(userRepo, jwtDict, authedUserDict, tokens)

  private final class AuthServiceInterpreter[F[_]: MonadThrow: Logger](
      userRepo: UserRepository[F],
      jwtDict: EphemeralDict[F, UserId, JwtToken],
      authedUserDict: EphemeralDict[F, JwtToken, UserId],
      tokens: JwtTokens[F]
  ) extends AuthService[F] {
    override def login(username: Username, password: Password): F[JwtToken] =
      userRepo
        .getByName(username)
        .flatMap { user =>
          if (passwordsMatch(user, password)) {
            jwtDict
              .get(user.id)
              .getOrElseF(tokens.create(user.id) flatTap { t =>
                jwtDict.store(user.id, t) *>
                  authedUserDict.store(t, user.id)
              })
          } else {
            Logger[F].warn(s"Invalid login attempt for user $username") *>
              InvalidPassword(username).raiseError[F, JwtToken]
          }
        }
        .onError { case e =>
          Logger[F].warn(e)(s"Attempt to login invalid used")
        }

    override def logout(uid: UserId, token: JwtToken): F[Unit] =
      jwtDict.delete(uid) *> authedUserDict.delete(token)

    override def authed(token: JwtToken): OptionT[F, AuthedUser] =
      authedUserDict
        .get(token)
        .map(AuthedUser.apply)
        .onError { case e =>
          OptionT.liftF(Logger[F].warn(e)("Invalid authentication attempt"))
        }
  }

  private def passwordsMatch(user: User, str: Password): Boolean =
    user.hashedPassword == PasswordHashing.hashSaltPassword(str, user.salt)
}
