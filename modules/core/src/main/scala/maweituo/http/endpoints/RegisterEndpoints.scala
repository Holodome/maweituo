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

trait RegisterEndpointDefs(using builder: EndpointBuilderDefs):
  val `post /register` =
    builder.public
      .post
      .in("register")
      .in(jsonBody[RegisterRequestDto])
      .out(jsonBody[RegisterResponseDto])

final class RegisterEndpoints[F[_]: MonadThrow](userService: UserService[F])(using EndpointsBuilder[F])
    extends RegisterEndpointDefs with Endpoints[F]:

  override val endpoints = List(
    `post /register`.serverLogic { register =>
      userService
        .create(register.toDomain)
        .map(RegisterResponseDto.apply)
        .toOut
    }
  ).map(_.tag("users"))
