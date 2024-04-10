package com.holodome.http

import cats.MonadError
import com.holodome.domain.errors._
import org.http4s.{HttpRoutes, Response}
import org.http4s.dsl.Http4sDsl
import com.olegpy.meow.hierarchy._

final class ApplicationErrorHandler[F[_]](implicit M: MonadError[F, ApplicationError])
    extends HttpErrorHandler[F, ApplicationError]
    with Http4sDsl[F] {

  private def handler: ApplicationError => F[Response[F]] = {
    case InvalidUserId() | InvalidChatId() | InvalidImageId() | InvalidAdId(_) => BadRequest()
    case InvalidAccess()                                                       => Forbidden()
    case CannotCreateChatWithMyself()                                          => Forbidden()
    case ChatAlreadyExists()                                                   => Conflict()
    case NotAnAuthor() | ChatAccessForbidden()                                 => Forbidden()
    case InternalImageUnsync()                                                 => InternalServerError()
    case InvalidPassword(_)                                                    => Forbidden()
    case NoUserFound(_)                                                        => NotFound()
    case UserEmailInUse(_) | UserNameInUse(_)                                  => Conflict()
  }

  override def handle(routes: HttpRoutes[F]): HttpRoutes[F] =
    RoutesHttpErrorHandler(routes)(handler)
}

object ApplicationErrorHandler {
  implicit def instance[F[_]](implicit
      M: MonadError[F, Throwable]
  ): HttpErrorHandler[F, ApplicationError] = new ApplicationErrorHandler
}
