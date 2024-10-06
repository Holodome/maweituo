package maweituo.interp

import cats.MonadThrow
import cats.data.OptionT
import cats.syntax.all.*

import maweituo.auth.{JwtTokens, PasswordHashing}
import maweituo.domain.errors.InvalidPassword
import maweituo.domain.users.*
import maweituo.domain.users.repos.UserRepo
import maweituo.domain.users.services.AuthService
import maweituo.infrastructure.EphemeralDict

import dev.profunktor.auth.jwt.JwtToken
import org.typelevel.log4cats.Logger

object AuthServiceInterp:
  def make[F[_]: MonadThrow: Logger](
      userRepo: UserRepo[F],
      jwtDict: EphemeralDict[F, UserId, JwtToken],
      authedUserDict: EphemeralDict[F, JwtToken, UserId],
      tokens: JwtTokens[F]
  ): AuthService[F] = new:
    def login(req: LoginRequest): F[LoginResponse] =
      val name     = req.name
      val password = req.password
      userRepo
        .getByName(name)
        .flatMap { user =>
          if passwordsMatch(user, password) then
            jwtDict
              .get(user.id)
              .getOrElseF(
                tokens.create(user.id)
                  .flatTap { t =>
                    jwtDict.store(user.id, t) *>
                      authedUserDict.store(t, user.id)
                  }
              )
              .map(t => LoginResponse(user.id, t))
          else
            Logger[F].warn(s"Invalid login attempt for user $name") *>
              InvalidPassword(name).raiseError[F, LoginResponse]
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
