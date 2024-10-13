package maweituo
package http

import cats.Monad
import cats.effect.kernel.Async
import cats.syntax.all.*

import maweituo.domain.services.users.AuthService

import dev.profunktor.auth.jwt.JwtToken
import org.http4s.HttpRoutes
import sttp.capabilities.fs2.Fs2Streams
import sttp.model.StatusCode
import sttp.model.headers.WWWAuthenticateChallenge
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter

export maweituo.http.dto.{*, given}
export maweituo.http.vars.*
export maweituo.http.errors.*
export maweituo.http.codecs.{*, given}

class RoutesBuilder[F[_]: Monad](authService: AuthService[F]):
  val base = endpoint
    .errorOut(statusCode and jsonBody[ErrorResponseDto])
  val public = base
  val authed = base
    .securityIn(auth.bearer[String](WWWAuthenticateChallenge.bearer))
    .serverSecurityLogic {
      bearer =>
        authService.authed(JwtToken(bearer)).value.map(Either.fromOption(
          _,
          // TODO: Move to DomainError
          (StatusCode.Unauthorized, ErrorResponseDto(List("unauthorized")))
        ))
    }

trait Endpoints[F[_]]:
  def endpoints: List[ServerEndpoint[Fs2Streams[F], F]]

object Endpoints:
  extension [F[_]: Async](e: Endpoints[F])
    def routes: HttpRoutes[F] = Http4sServerInterpreter[F]().toRoutes(e.endpoints)
