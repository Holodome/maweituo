package com.holodome.repositories

import com.holodome.domain.users.UserId
import dev.profunktor.auth.jwt.JwtToken

trait AuthedUserRepository[F[_]] extends DictionaryRepository[F, JwtToken, UserId]
