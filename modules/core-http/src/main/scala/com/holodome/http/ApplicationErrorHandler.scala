package com.holodome.http

import cats.syntax.all._
import cats.{MonadError, MonadThrow}
import com.holodome.domain.errors._
import com.olegpy.meow.hierarchy._
import io.circe.Encoder
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Response}
import org.typelevel.log4cats.Logger

final class ApplicationErrorHandler[F[_]: Logger](implicit M: MonadError[F, ApplicationError])
    extends HttpErrorHandler[F, ApplicationError]
    with Http4sDsl[F] {

  private case class ErrorWrapper(message: String)

  private implicit val errorWrapperEncoder: Encoder[ErrorWrapper] =
    Encoder.forProduct1("errors")(_.message)

  private def handler: ApplicationError => F[Response[F]] = error =>
    Logger[F].error(error)(s"Application error") *> {
      error match {
        case InvalidChatId(c)      => BadRequest(ErrorWrapper(s"Invalid chat id $c"))
        case InvalidImageId(c)     => BadRequest(ErrorWrapper(s"Invalid image id $c"))
        case InvalidAdId(c)        => BadRequest(ErrorWrapper(s"Invalid ad id $c"))
        case InvalidUserId(c)      => BadRequest(ErrorWrapper(s"Invalid user id $c"))
        case InvalidAccess(reason) => Forbidden(ErrorWrapper(s"Invalid access: $reason"))
        case CannotCreateChatWithMyself(a, u) =>
          Forbidden(ErrorWrapper(s"Can't create chat with myself $u for ad $a"))
        case ChatAlreadyExists(c, u) =>
          Conflict(ErrorWrapper(s"Chat for ad $c with user $u already exists"))
        case NotAnAuthor(c, u)        => Forbidden(ErrorWrapper(s"User $u is not author of $c"))
        case ChatAccessForbidden(c)   => Forbidden(ErrorWrapper(s"Access to chat $c is forbidden"))
        case InternalImageUnsync(_)   => InternalServerError()
        case InvalidPassword(u)       => Forbidden(ErrorWrapper(s"Invalid password for user $u"))
        case NoUserFound(u)           => Forbidden(ErrorWrapper(s"User with name $u not found"))
        case UserEmailInUse(e)        => Conflict(ErrorWrapper(s"Email $e is already taken"))
        case UserNameInUse(n)         => Conflict(ErrorWrapper(s"Name $n is already taken"))
        case DatabaseEncodingError(_) => InternalServerError()
        case FeedError(_)             => InternalServerError()
      }
    }

  override def handle(routes: HttpRoutes[F]): HttpRoutes[F] =
    RoutesHttpErrorHandler(routes)(handler)
}

object ApplicationErrorHandler {
  implicit def instance[F[_]: Logger: MonadThrow]: HttpErrorHandler[F, ApplicationError] =
    new ApplicationErrorHandler
}
