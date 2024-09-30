package com.holodome.interpreters

import com.holodome.auth.{ JwtTokens, PasswordHashing }
import com.holodome.domain.errors.InvalidPassword
import com.holodome.domain.users.*
import com.holodome.domain.users.repos.UserRepository
import com.holodome.domain.users.services.AuthService
import com.holodome.infrastructure.EphemeralDict

import cats.MonadThrow
import cats.data.OptionT
import cats.syntax.all.*
import dev.profunktor.auth.jwt.JwtToken
import org.typelevel.log4cats.Logger

object AuthServiceInterpreter:
  def make[F[_]: MonadThrow: Logger](
      userRepo: UserRepository[F],
      jwtDict: EphemeralDict[F, UserId, JwtToken],
      authedUserDict: EphemeralDict[F, JwtToken, UserId],
      tokens: JwtTokens[F]
  ): AuthService[F] = new:
    def login(username: Username, password: Password): F[(JwtToken, UserId)] =
      userRepo
        .getByName(username)
        .flatMap { user =>
          if passwordsMatch(user, password) then
            jwtDict
              .get(user.id)
              .getOrElseF(tokens.create(user.id) flatTap { t =>
                jwtDict.store(user.id, t) *>
                  authedUserDict.store(t, user.id)
              })
              .map(t => (t, user.id))
          else
            Logger[F].warn(s"Invalid login attempt for user $username") *>
              InvalidPassword(username).raiseError[F, (JwtToken, UserId)]
        }
        .onError { case e =>
          Logger[F].warn(e)(s"Attempt to login invalid used")
        }

    def logout(uid: UserId, token: JwtToken): F[Unit] =
      jwtDict.delete(uid) *> authedUserDict.delete(token)

    def authed(token: JwtToken): OptionT[F, AuthedUser] =
      authedUserDict
        .get(token)
        .map(AuthedUser.apply)
        .onError { case e =>
          OptionT.liftF(Logger[F].warn(e)("Invalid authentication attempt"))
        }

    private def passwordsMatch(user: User, str: Password): Boolean =
      user.hashedPassword == PasswordHashing.hashSaltPassword(str, user.salt)
