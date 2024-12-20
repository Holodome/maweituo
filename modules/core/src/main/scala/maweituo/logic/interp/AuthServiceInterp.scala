package maweituo
package logic
package interp

import cats.MonadThrow
import cats.data.OptionT
import cats.syntax.all.*

import maweituo.domain.all.*
import maweituo.infrastructure.EphemeralDict
import maweituo.logic.auth.{JwtTokens, PasswordHashing}

import dev.profunktor.auth.jwt.JwtToken
import org.typelevel.log4cats.{Logger, LoggerFactory}

object AuthServiceInterp:
  def make[F[_]: MonadThrow: LoggerFactory](
      userRepo: UserRepo[F],
      jwtDict: EphemeralDict[F, UserId, JwtToken],
      authedUserDict: EphemeralDict[F, JwtToken, UserId],
      tokens: JwtTokens[F]
  ): AuthService[F] = new:
    private given Logger[F] = LoggerFactory[F].getLogger

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
              DomainError.InvalidPassword(name).raiseError[F, LoginResponse]
        }
        .onError { case e =>
          Logger[F].warn(e)(s"Attempt to login invalid used")
        }

    def logout(token: JwtToken)(using id: Identity): F[Unit] =
      jwtDict.delete(id) *> authedUserDict.delete(token)

    def authed(token: JwtToken): OptionT[F, AuthedUser] =
      authedUserDict
        .get(token)
        .map(AuthedUser(_, token))
        .onError { case e =>
          OptionT.liftF(Logger[F].warn(e)("Invalid authentication attempt"))
        }

    private def passwordsMatch(user: User, str: Password): Boolean =
      user.hashedPassword == PasswordHashing.hashSaltPassword(str, user.salt)
