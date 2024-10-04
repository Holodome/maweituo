package maweituo.e2e

import cats.effect.IO

import maweituo.domain.ads.{AdId, AdTag, AddTagRequest, Advertisement, CreateAdRequest}
import maweituo.domain.users.*
import maweituo.utils.given

import dev.profunktor.auth.jwt.JwtToken
import org.http4s.*
import org.http4s.Method.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.client.*
import org.http4s.headers.{Accept, Authorization}

final class AppClient(base: String, client: Client[IO]):
  private def onError(resp: Response[IO]) =
    resp.body.compile.toList.map { body =>
      new RuntimeException(f"got unexpected response ${resp.toString} with body '${body.map(_.toChar).mkString}'")
    }

  def makeUri(subpath: String): Uri = Uri.unsafeFromString(f"$base/$subpath")

  def login(body: LoginRequest): IO[JwtToken] =
    client.expectOr[JwtToken](
      Request[IO](
        method = Method.POST,
        uri = makeUri("login")
      ).withEntity(body)
    )(onError)

  def register(body: RegisterRequest): IO[Unit] =
    client.expectOr[AdId](
      Request[IO](
        method = Method.POST,
        uri = makeUri("register")
      ).withEntity(body)
    )(onError).void

  def createAd(body: CreateAdRequest)(using jwt: JwtToken): IO[AdId] =
    client.expectOr[AdId](
      Request[IO](
        method = Method.POST,
        uri = makeUri("ads"),
        headers = Headers(
          Authorization(Credentials.Token(AuthScheme.Bearer, jwt.value)),
          Accept(MediaType.application.json)
        )
      ).withEntity(body)
    )(onError)

  def addTag(ad: AdId, tag: AddTagRequest)(using jwt: JwtToken): IO[Unit] =
    client.expectOr[Unit](
      Request[IO](
        method = Method.POST,
        uri = makeUri(f"ads/$ad/tag"),
        headers = Headers(
          Authorization(Credentials.Token(AuthScheme.Bearer, jwt.value)),
          Accept(MediaType.application.json)
        )
      ).withEntity(tag)
    )(onError)

  def getAd(ad: AdId): IO[Advertisement] =
    client.expectOr[Advertisement](makeUri(f"ads/$ad"))(onError)

  def getAdTags(ad: AdId): IO[List[AdTag]] =
    client.expectOr[List[AdTag]](makeUri(f"ads/$ad/tag"))(onError)
