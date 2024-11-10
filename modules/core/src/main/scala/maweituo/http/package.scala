package maweituo
package http

import cats.effect.kernel.Async
import cats.syntax.all.*
import cats.{Monad, MonadThrow}

import maweituo.domain.exports.Identity
import maweituo.domain.services.users.AuthService
import maweituo.domain.users.AuthedUser

import dev.profunktor.auth.jwt.JwtToken
import org.http4s.HttpRoutes
import sttp.capabilities.fs2.Fs2Streams
import sttp.model.StatusCode
import sttp.model.headers.WWWAuthenticateChallenge
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.server.{PartialServerEndpoint, ServerEndpoint}

export maweituo.http.dto.{*, given}
export maweituo.http.vars.*
export maweituo.http.errors.*
export maweituo.http.codecs.{*, given}

type ErrorResponseData = (StatusCode, ErrorResponseDto)

class EndpointBuilderDefs:
  def base   = endpoint.errorOut(statusCode and jsonBody[ErrorResponseDto]).in("api" / "v1")
  def public = base
  def authed = base.securityIn(auth.bearer[JwtToken](WWWAuthenticateChallenge.bearer))

class EndpointsBuilder[F[_]: Monad](authService: AuthService[F]) extends EndpointBuilderDefs:
  def secure[I, O, R](e: Endpoint[JwtToken, I, ErrorResponseData, O, R])
      : PartialServerEndpoint[JwtToken, AuthedUser, I, (StatusCode, ErrorResponseDto), O, R, F] =
    e.serverSecurityLogic {
      bearer =>
        authService.authed(bearer).value.map(Either.fromOption(
          _,
          // TODO: Move to DomainError
          (StatusCode.Unauthorized, ErrorResponseDto(List("unauthorized")))
        ))
    }

extension [I, O, R](e: Endpoint[Unit, I, ErrorResponseData, O, R])
  def serverLogicF[F[_]: MonadThrow](fn: I => F[O]) =
    e.serverLogic { i => fn(i).toOut }

extension [I, O, R](e: Endpoint[JwtToken, I, ErrorResponseData, O, R])
  def secure[F[_]](using builder: EndpointsBuilder[F]) =
    builder.secure(e)

  def authedServerLogic[F[_]: MonadThrow](fn: Identity ?=> I => F[O])(using builder: EndpointsBuilder[F]) =
    e.secure.serverLogic { authed => i =>
      given Identity = Identity(authed.id)
      fn(i).toOut
    }

  def secureServerLogic[F[_]: MonadThrow](fn: AuthedUser => I => F[O])(using builder: EndpointsBuilder[F]) =
    e.secure.serverLogic { authed => i =>
      fn(authed)(i).toOut
    }

trait Endpoints[F[_]]:
  def endpoints: List[ServerEndpoint[Fs2Streams[F], F]]

object Endpoints:
  extension [F[_]: Async](e: Endpoints[F])
    def routes: HttpRoutes[F] = Http4sServerInterpreter[F]().toRoutes(e.endpoints)
