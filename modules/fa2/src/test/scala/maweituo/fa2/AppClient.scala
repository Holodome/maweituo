package maweituo.fa2

import org.http4s.client.Client
import cats.effect.IO
import org.http4s.Method
import org.http4s.Request
import org.http4s.implicits.*

class AppClient(client: Client[IO]):
  def login(dto: LoginDTO): IO[String] =
    client.expect[String](Request[IO](Method.POST, uri"http://localhost:8080/login"))

  def verifyLogin(dto: VerifyLoginDTO): IO[String] =
    client.expect[String](Request[IO](Method.POST, uri"http://localhost:8080/login_verify"))

  def password(dto: ResetPasswordDTO): IO[String] =
    client.expect[String](Request[IO](Method.POST, uri"http://localhost:8080/reset_password"))

  def verifyPassword(dto: VerifyResetPasswordDTO): IO[String] =
    client.expect[String](Request[IO](Method.POST, uri"http://localhost:8080/verify_reset_password"))
