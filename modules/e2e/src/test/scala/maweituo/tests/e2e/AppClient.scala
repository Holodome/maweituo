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
import sttp.model.StatusCode
import sttp.tapir.client.http4s.Http4sClientInterpreter
import sttp.tapir.{Endpoint, PublicEndpoint}

object AppClient:
  extension [O](r: IO[Either[ErrorResponseData, O]])
    def unwrap = r.flatMap {
      case Left(e)      => IO.raiseError(new RuntimeException(s"got unexpected response ${e}"))
      case Right(value) => IO.pure(value)
    }

final class AppClient(base: String, client: Client[IO]):
  given EndpointBuilderDefs = new EndpointBuilderDefs {}

  private val authEndpoints     = new AuthEndpointDefs
  private val registerEndpoints = new RegisterEndpointDefs
  private val feedEndpoints     = new FeedEndpointDefs
  private val tagEndpoints      = new TagEndpointDefs
  private val userAdEndpoints   = new UserAdEndpointDefs
  private val userChatEndpoints = new UserChatEndpointDefs
  private val userEndpoints     = new UserEndpointDefs
  private val adChatEndpoints   = new AdChatEndpointDefs
  private val adEndpoints       = new AdEndpointDefs
  private val adImageEndpoints  = new AdImageEndpointDefs
  private val adMsgEndpoints    = new AdMsgEndpointDefs
  private val adTagEndpoints    = new AdTagEndpointDefs

  private def publicMethod[I, O, R](endpoint: PublicEndpoint[I, ErrorResponseData, O, R])
      : I => IO[Either[ErrorResponseData, O]] = dto =>
    lazy val fn = Http4sClientInterpreter[IO]().toRequestThrowDecodeFailures(
      endpoint,
      baseUri = Some(Uri.unsafeFromString(base))
    )
    val (req, respFn) = fn(dto)
    client.run(req).use(respFn)

  final class PartiallyAppliedAuthedMethod[I, O, R](fn: JwtToken => I => IO[Either[ErrorResponseData, O]]):
    def apply(using a: JwtToken) = fn(a)

  private def authedMethod[I, O, R](endpoint: Endpoint[JwtToken, I, ErrorResponseData, O, R])
      : PartiallyAppliedAuthedMethod[I, O, R] =
    val internal = (jwt: JwtToken) =>
      (dto: I) =>
        lazy val fn = Http4sClientInterpreter[IO]().toSecureRequestThrowDecodeFailures(
          endpoint,
          baseUri = Some(Uri.unsafeFromString(base))
        )
        val (req, respFn) = fn(jwt)(dto)
        client.run(req).use(respFn)
    PartiallyAppliedAuthedMethod(internal)

  val `post /login`                        = publicMethod(authEndpoints.loginEndpoint)
  val `post /register`                     = publicMethod(registerEndpoints.registerEndpoint)
  val `get /ads/$adId`                     = publicMethod(adEndpoints.getAdEndpoint)
  val `post /ads`                          = authedMethod(adEndpoints.createAdEndpoint)
  val `delete /ads`                        = authedMethod(adEndpoints.deleteAdEndpoint)
  val `put /ads`                           = authedMethod(adEndpoints.updateAdEndpoint)
  val `post /ads/$adId/tags`               = authedMethod(adTagEndpoints.addAdTagEndpoint)
  val `delete /ads/$adId/tags`             = authedMethod(adTagEndpoints.deleteAdTagEndpoint)
  val `get /ads/$adId/tags`                = publicMethod(adTagEndpoints.getAdTagsEndpoint)
  val `get /feed`                          = publicMethod(feedEndpoints.publicFeedEndpoint)
  val `get /feed/$userId`                  = authedMethod(feedEndpoints.userFeedEndpoint)
  val `get /tags`                          = publicMethod(tagEndpoints.getAllTagsEndpoint)
  val `get /tags/$tag/ads`                 = publicMethod(tagEndpoints.getTagAdsEndpoint)
  val `get /users/$userId`                 = publicMethod(userEndpoints.getUserEndpoint)
  val `delete /users/$userId`              = authedMethod(userEndpoints.deleteUserEndpoint)
  val `put /users/$userId`                 = authedMethod(userEndpoints.updateUserEndpoint)
  val `get /users/$userId/ads`             = publicMethod(userAdEndpoints.getUserAdsEndpoint)
  val `get /users/$userId/chats`           = authedMethod(userChatEndpoints.getUserChatsEndpoint)
  val `get /ads/$adId/chats/$chatId`       = authedMethod(adChatEndpoints.getChatEndpoint)
  val `get /ads/$adId/chats`               = authedMethod(adChatEndpoints.getChatsEndpoint)
  val `post /ads/$adId/chats`              = authedMethod(adChatEndpoints.createChatEndpoint)
  val `get /ads/$adId/imgs/$imgId`         = publicMethod(adImageEndpoints.getImageEndpoint)
  val `post /ads/$adId/imgs`               = authedMethod(adImageEndpoints.createImageEndpoint)
  val `delete /ads/$adId/imgs/$imgId`      = authedMethod(adImageEndpoints.deleteImageEndpoint)
  val `get /ads/$adId/chats/$chatId/msgs`  = authedMethod(adMsgEndpoints.getMessagesEndpoint)
  val `post /ads/$adId/chats/$chatId/msgs` = authedMethod(adMsgEndpoints.sendMessageEndpoint)
