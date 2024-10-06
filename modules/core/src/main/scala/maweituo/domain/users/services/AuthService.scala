package maweituo.domain.users.services

import cats.data.OptionT

import maweituo.domain.Identity
import maweituo.domain.users.*

import dev.profunktor.auth.jwt.JwtToken

trait AuthService[F[_]]:
  def login(req: LoginRequest): F[LoginResponse]
  def logout(token: JwtToken)(using Identity): F[Unit]
  def authed(token: JwtToken): OptionT[F, AuthedUser]
