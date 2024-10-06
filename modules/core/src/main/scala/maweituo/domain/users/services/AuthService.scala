package maweituo.domain.users.services

import cats.data.OptionT

import maweituo.domain.users.*

import dev.profunktor.auth.jwt.JwtToken

trait AuthService[F[_]]:
  def login(req: LoginRequest): F[LoginResponse]
  def logout(uid: UserId, token: JwtToken): F[Unit]
  def authed(token: JwtToken): OptionT[F, AuthedUser]
