package maweituo.tests.utils

import cats.Show
import cats.derived.*
import cats.syntax.all.*

import maweituo.domain.users.{LoginRequest, RegisterRequest, UpdateUserRepoRequest, UpdateUserRequest, User}
import maweituo.utils.given

import dev.profunktor.auth.jwt.JwtToken

given Show[User]                  = Show.derived
given Show[LoginRequest]          = Show.derived
given Show[RegisterRequest]       = Show.derived
given Show[UpdateUserRequest]     = Show.derived
given Show[UpdateUserRepoRequest] = Show.derived
given Show[JwtToken]              = Show[String].contramap[JwtToken](_.value)
