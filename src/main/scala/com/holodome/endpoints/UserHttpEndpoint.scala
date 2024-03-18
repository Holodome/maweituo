package com.holodome.endpoints

import cats.data.{EitherT, Reader}
import cats.effect.{Effect, IO}
import com.holodome.models.auth.Login
import com.holodome.models.{LoginError, User}
import com.holodome.repositories.UserRepository
import com.holodome.services.UserService
import sttp.tapir.endpoint
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._

class UserHttpEndpoint[F[_]: Effect](
    userService: UserService[F],
    userRepository: UserRepository[F]
) {
  private val loginEndpoint: ServerEndpoint[Login.Request, _, Unit, Any, F] =
    endpoint.post
      .in("login")
      .in(jsonBody[Login.Request])
      .errorOut(jsonBody[LoginError])
      .serverLogic(body => userService.login(body).run(userRepository).value)

  val endpoints = List(loginEndpoint)
}
