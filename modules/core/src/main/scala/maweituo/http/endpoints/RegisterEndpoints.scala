package maweituo
package http
package endpoints
import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

final class RegisterEndpoints[F[_]: MonadThrow](userService: UserService[F])(using builder: RoutesBuilder[F])
    extends Endpoints[F]:

  val registerEndpoint =
    builder.public
      .post
      .in("register")
      .in(jsonBody[RegisterRequestDto])
      .out(jsonBody[RegisterResponseDto])
      .serverLogic { register =>
        userService
          .create(register.toDomain)
          .map(RegisterResponseDto.apply)
          .toOut
      }

  override val endpoints = List(
    registerEndpoint
  ).map(_.tag("users"))
