package com.holodome.http.routes

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.errors.ApplicationError
import com.holodome.domain.users.RegisterRequest
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.http.HttpErrorHandler
import com.holodome.services.UserService
import org.http4s.{HttpRoutes, Uri}
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.Uri.Path.Segment

final case class RegisterRoutes[F[_]: MonadThrow: JsonDecoder](userService: UserService[F])(implicit
    H: HttpErrorHandler[F, ApplicationError]
) extends Http4sDsl[F] {

  val routes: HttpRoutes[F] = H.handle(HttpRoutes.of { case req @ POST -> Root / "register" =>
    req.decodeR[RegisterRequest] { register =>
      userService
        .register(register)
        .flatMap(uid =>
          Created().map {
            val header = Location(Uri(path = Root / Segment("users") / Segment(uid.toString)))
            _.putHeaders(header)
          }
        )
    }
  })
}
