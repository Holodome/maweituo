package com.holodome.repositories

import cats.data.OptionT
import com.holodome.domain.users._
import dev.profunktor.auth.jwt.JwtToken

trait JwtRepository[F[_]] {
  def storeToken(username: Username, token: JwtToken): F[Unit]
  def getToken(username: Username): OptionT[F, JwtToken]
  def deleteToken(username: Username): F[Unit]
}
