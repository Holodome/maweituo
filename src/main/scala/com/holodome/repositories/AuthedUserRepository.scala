package com.holodome.repositories

import com.holodome.domain.users.Username
import dev.profunktor.auth.jwt.JwtToken

trait AuthedUserRepository[F[_]] extends DictionaryRepository[F, JwtToken, Username]
