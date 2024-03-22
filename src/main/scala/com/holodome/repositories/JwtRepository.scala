package com.holodome.repositories

import com.holodome.domain.users._
import dev.profunktor.auth.jwt.JwtToken

trait JwtRepository[F[_]] extends DictionaryRepository[F, Username, JwtToken]
