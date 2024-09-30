package maweituo.domain.users.services

import maweituo.domain.users.*

import cats.data.OptionT
import dev.profunktor.auth.jwt.JwtToken

trait AuthService[F[_]]:
  def login(username: Username, password: Password): F[(JwtToken, UserId)]
  def logout(uid: UserId, token: JwtToken): F[Unit]
  def authed(token: JwtToken): OptionT[F, AuthedUser]
