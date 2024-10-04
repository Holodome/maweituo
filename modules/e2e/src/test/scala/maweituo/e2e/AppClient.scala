package maweituo.e2e

import cats.effect.IO

import maweituo.domain.ads.{AdId, AdTag, Advertisement, CreateAdRequest}
import maweituo.domain.users.*
import maweituo.utils.given

import dev.profunktor.auth.jwt.JwtToken
import org.http4s.*
import org.http4s.Method.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.client.*
import org.http4s.headers.{Accept, Authorization}

final class AppClient(uri: Uri, client: Client[IO]):
  def makeUrl(subpath: String): Uri = uri.withPath("/" + subpath)

  def login(body: LoginRequest): IO[JwtToken] =
    client.expect[JwtToken](
      Request[IO](
        method = Method.POST,
        uri = makeUrl("login")
      ).withEntity(body)
    )

  def createAd(body: CreateAdRequest)(using jwt: JwtToken): IO[AdId] =
    client.expect[AdId](
      Request[IO](
        method = Method.POST,
        uri = makeUrl("ads"),
        headers = Headers(
          Authorization(Credentials.Token(AuthScheme.Bearer, jwt.value)),
          Accept(MediaType.application.json)
        )
      )
    )

  def addTag(ad: AdId, tag: AdTag)(using jwt: JwtToken): IO[Unit] =
    client.expect[Unit](
      Request[IO](
        method = Method.POST,
        uri = makeUrl(f"ads/$ad/tags"),
        headers = Headers(
          Authorization(Credentials.Token(AuthScheme.Bearer, jwt.value)),
          Accept(MediaType.application.json)
        )
      )
    )

  def getAd(ad: AdId): IO[Advertisement] =
    client.expect[Advertisement](makeUrl(f"ads/$ad"))

  def getAdTags(ad: AdId): IO[List[AdTag]] =
    client.expect[List[AdTag]](makeUrl(f"ads/$ad/tags"))
