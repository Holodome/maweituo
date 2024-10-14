package maweituo
package tests
package e2e

import maweituo.domain.all.MediaType as _
import maweituo.http.dto.*
import maweituo.http.endpoints.*
import maweituo.http.endpoints.ads.*
import maweituo.http.endpoints.users.*
import maweituo.http.{EndpointBuilderDefs, ErrorResponseData}

import dev.profunktor.auth.jwt.JwtToken
import org.http4s.*
import org.http4s.client.*
import org.typelevel.log4cats.Logger
import sttp.model.StatusCode
import sttp.tapir.client.http4s.Http4sClientInterpreter
import sttp.tapir.{Endpoint, PublicEndpoint}

final class MaweituoApiClient(base: String, client: Client[IO])(using LoggerFactory[IO]):
  given EndpointBuilderDefs = new EndpointBuilderDefs {}
  given Logger[IO]          = LoggerFactory[IO].getLogger

  trait AllEndpoints extends AuthEndpointDefs with RegisterEndpointDefs with FeedEndpointDefs with TagEndpointDefs
      with UserAdEndpointDefs with UserChatEndpointDefs with UserEndpointDefs with AdChatEndpointDefs
      with AdEndpointDefs with AdImageEndpointDefs[IO] with AdMsgEndpointDefs with AdTagEndpointDefs
  private val endpoints = new AllEndpoints {}

  private def unwrap[O](r: IO[Either[ErrorResponseData, O]]): IO[O] = r.flatMap {
    case Left(e)      => IO.raiseError(new RuntimeException(s"got unexpected response ${e}"))
    case Right(value) => IO.pure(value)
  }

  final class PartiallyAppliedMethod[I, O](fn: I => IO[Either[ErrorResponseData, O]]):
    def attempt(i: I): IO[Either[ErrorResponseData, O]] = fn(i)
    def apply(i: I): IO[O]                              = unwrap(attempt(i))

  final class PartiallyAppliedAuthedMethod[I, O](fn: JwtToken => I => IO[Either[ErrorResponseData, O]]):
    def apply(i: I)(using a: JwtToken): IO[O]                              = unwrap(attempt(i))
    def attempt(i: I)(using a: JwtToken): IO[Either[ErrorResponseData, O]] = fn(a)(i)

  private def publicMethod[I, O, R](endpoint: PublicEndpoint[I, ErrorResponseData, O, R])
      : PartiallyAppliedMethod[I, O] =
    val internal =
      (dto: I) =>
        lazy val fn = Http4sClientInterpreter[IO]().toRequestThrowDecodeFailures(
          endpoint,
          baseUri = Some(Uri.unsafeFromString(base))
        )
        val (req, respFn) = fn(dto)
        Logger[IO].info(s"sending request $req") *> client.run(req).use { resp =>
          Logger[IO].info(s"got response $resp") *> respFn(resp)
        }
    PartiallyAppliedMethod(internal)

  private def authedMethod[I, O, R](endpoint: Endpoint[JwtToken, I, ErrorResponseData, O, R])
      : PartiallyAppliedAuthedMethod[I, O] =
    val internal = (jwt: JwtToken) =>
      (dto: I) =>
        lazy val fn = Http4sClientInterpreter[IO]().toSecureRequestThrowDecodeFailures(
          endpoint,
          baseUri = Some(Uri.unsafeFromString(base))
        )
        val (req, respFn) = fn(jwt)(dto)
        Logger[IO].info(s"sending request $req") *> client.run(req).use { resp =>
          Logger[IO].info(s"got response $resp") *> respFn(resp)
        }
    PartiallyAppliedAuthedMethod(internal)

  val `post /login`                        = publicMethod(endpoints.`post /login`)
  val `post /register`                     = publicMethod(endpoints.`post /register`)
  val `post /logout`                       = authedMethod(endpoints.`post /logout`)
  val `get /ads/$adId`                     = publicMethod(endpoints.`get /ads/$adId`)
  val `post /ads`                          = authedMethod(endpoints.`post /ads`)
  val `delete /ads/$adId`                  = authedMethod(endpoints.`delete /ads/$adId`)
  val `put /ads/$adId`                     = authedMethod(endpoints.`put /ads/$adId`)
  val `post /ads/$adId/tags`               = authedMethod(endpoints.`post /ads/$adId/tags`)
  val `delete /ads/$adId/tags`             = authedMethod(endpoints.`delete /ads/$adId/tags`)
  val `get /ads/$adId/tags`                = publicMethod(endpoints.`get /ads/$adId/tags`)
  val `get /feed`                          = publicMethod(endpoints.`get /feed`)
  val `get /feed/$userId`                  = authedMethod(endpoints.`get /feed/$userId`)
  val `get /tags`                          = publicMethod(endpoints.`get /tags`)
  val `get /tags/$tag/ads`                 = publicMethod(endpoints.`get /tags/$tag/ads`)
  val `get /users/$userId`                 = publicMethod(endpoints.`get /users/$userId`)
  val `delete /users/$userId`              = authedMethod(endpoints.`delete /users/$userId`)
  val `put /users/$userId`                 = authedMethod(endpoints.`put /users/$userId`)
  val `get /users/$userId/ads`             = publicMethod(endpoints.`get /users/$userId/ads`)
  val `get /users/$userId/chats`           = authedMethod(endpoints.`get /users/$userId/chats`)
  val `get /ads/$adId/chats/$chatId`       = authedMethod(endpoints.`get /ads/$adId/chats/$chatId`)
  val `get /ads/$adId/chats`               = authedMethod(endpoints.`get /ads/$adId/chats`)
  val `post /ads/$adId/chats`              = authedMethod(endpoints.`post /ads/$adId/chats`)
  val `get /ads/$adId/imgs/$imgId`         = publicMethod(endpoints.`get /ads/$adId/imgs/$imgId`)
  val `post /ads/$adId/imgs`               = authedMethod(endpoints.`post /ads/$adId/imgs`)
  val `delete /ads/$adId/imgs/$imgId`      = authedMethod(endpoints.`delete /ads/$adId/imgs/$imgId`)
  val `get /ads/$adId/chats/$chatId/msgs`  = authedMethod(endpoints.`get /ads/$adId/chats/$chatId/msgs`)
  val `post /ads/$adId/chats/$chatId/msgs` = authedMethod(endpoints.`post /ads/$adId/chats/$chatId/msgs`)
