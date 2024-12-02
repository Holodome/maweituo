package maweituo.fa2

import org.http4s.client.Client
import cats.effect.IO
import org.http4s.Method
import org.http4s.Request
import org.http4s.implicits.*

import io.circe.Codec
import com.comcast.ip4s.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.{CirceEntityCodec, JsonDecoder}
import org.http4s.dsl.Http4sDsl

class AppClient(client: Client[IO]):
  def login(dto: LoginDTO): IO[String] =
    client.expect[String](Request[IO](Method.POST, uri"http://localhost:8080/login").withEntity(dto))

  def verifyLogin(dto: VerifyLoginDTO): IO[String] =
    client.expect[String](Request[IO](Method.POST, uri"http://localhost:8080/login_verify").withEntity(dto))

  def password(dto: ResetPasswordDTO): IO[String] =
    client.expect[String](Request[IO](Method.POST, uri"http://localhost:8080/reset_password").withEntity(dto))

  def verifyPassword(dto: VerifyResetPasswordDTO): IO[String] =
    client.expect[String](Request[IO](Method.POST, uri"http://localhost:8080/verify_reset_password").withEntity(dto))
