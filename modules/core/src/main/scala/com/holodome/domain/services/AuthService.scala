package com.holodome.domain.services

import cats.data.OptionT
import com.holodome.domain.users._
import dev.profunktor.auth.jwt.JwtToken

trait AuthService[F[_]] {
  def login(username: Username, password: Password): F[(JwtToken, UserId)]
  def logout(uid: UserId, token: JwtToken): F[Unit]
  def authed(token: JwtToken): OptionT[F, AuthedUser]
}
