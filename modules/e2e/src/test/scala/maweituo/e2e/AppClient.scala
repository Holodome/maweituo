package maweituo.e2e

import cats.effect.IO

import maweituo.domain.ads.AdId
import maweituo.http.dto.*
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

  def login(body: LoginRequestDto): IO[LoginResponseDto] =
    client.expectOr[LoginResponseDto](
      Request[IO](
        method = Method.POST,
        uri = makeUri("login")
      ).withEntity(body)
    )(onError)

  def register(body: RegisterRequestDto): IO[Unit] =
    client.expectOr[RegisterResponseDto](
      Request[IO](
        method = Method.POST,
        uri = makeUri("register")
      ).withEntity(body)
    )(onError).void

  def createAd(body: CreateAdRequestDto)(using jwt: JwtToken): IO[CreateAdResponseDto] =
    client.expectOr[CreateAdResponseDto](
      Request[IO](
        method = Method.POST,
        uri = makeUri("ads"),
        headers = Headers(
          Authorization(Credentials.Token(AuthScheme.Bearer, jwt.value)),
          Accept(MediaType.application.json)
        )
      ).withEntity(body)
    )(onError)

  def addTag(ad: AdId, tag: AddTagRequestDto)(using jwt: JwtToken): IO[Unit] =
    client.successful(
      Request[IO](
        method = Method.POST,
        uri = makeUri(f"ads/$ad/tag"),
        headers = Headers(
          Authorization(Credentials.Token(AuthScheme.Bearer, jwt.value)),
          Accept(MediaType.application.json)
        )
      ).withEntity(tag)
    ).flatMap {
      case true  => IO.unit
      case false => IO.raiseError(new RuntimeException("no success"))
    }

  def getAd(ad: AdId): IO[AdDto] =
    client.expectOr[AdDto](makeUri(f"ads/$ad"))(onError)

  def getAdTags(ad: AdId): IO[AdTagsResponseDto] =
    client.expectOr[AdTagsResponseDto](makeUri(f"ads/$ad/tag"))(onError)
