package maweituo
package http

import cats.data.Kleisli
import cats.effect.Concurrent

import maweituo.domain.users.UserId
import maweituo.http.dto.ErrorResponseDto
import maweituo.logic.errors.DomainError

import org.http4s.circe.CirceEntityCodec.given
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Request, Response}
import org.typelevel.log4cats.Logger

object errors:
  private final case class ErrorHandler[F[_]: Concurrent: Logger]() extends Http4sDsl[F]:
    private def notFound(error: String)  = NotFound(ErrorResponseDto.make(error))
    private def forbidden(error: String) = Forbidden(ErrorResponseDto.make(error))
    private def conflict(error: String)  = Conflict(ErrorResponseDto.make(error))
    // private def badRequest(error: String) = BadRequest(ErrorResponseDto.make(error))

    private def errorToResponse(error: DomainError): F[Response[F]] =
      error match
        case DomainError.UserModificationForbidden(violator) =>
          forbidden(s"user modification forbidden by user $violator")
        case DomainError.InvalidUserId(userId)       => notFound(s"invalid user id $userId")
        case DomainError.NoUserWithName(username)    => notFound(s"no user with name $username found")
        case DomainError.NoUserWithEmail(email)      => notFound(s"no user with email $email found")
        case DomainError.UserNameInUse(username)     => conflict(s"name $username is aleady taken")
        case DomainError.UserEmailInUse(email)       => conflict(s"email $email is already taken")
        case DomainError.InvalidPassword(username)   => forbidden(s"invalid password for user $username")
        case DomainError.InvalidChatId(chatId)       => notFound(s"invalid chat id $chatId")
        case DomainError.ChatAccessForbidden(chatId) => forbidden(s"access to chat $chatId forbidden")
        case DomainError.InvalidImageId(imageId)     => notFound(s"invalid image id $imageId")
        case DomainError.InternalImageUnsync(reason) => InternalServerError()
        case DomainError.InvalidAdId(id)             => notFound(s"invalid ad id $id")
        case DomainError.CannotCreateChatWithMyself(adId, user) =>
          conflict(s"can't create chat with yourself for user $user ad $adId")
        case DomainError.ChatAlreadyExists(adId, clientId) =>
          conflict(s"chat for ad $adId with user $clientId already exists")
        case DomainError.AdModificationForbidden(adId, userId) =>
          forbidden(s"ad $adId modification by user $userId forbidden")
        case DomainError.InvalidSearchParams(errors) => BadRequest(ErrorResponseDto(errors.map(_.show)))

    def httpDomainErrorHandler(routes: HttpRoutes[F]): HttpRoutes[F] =
      Kleisli { (req: Request[F]) =>
        OptionT(
          routes.run(req).value.recoverWith {
            case e: DomainError => Logger[F].error(e)("domain error") *> errorToResponse(e).map(_.some)
          }
        )
      }

  object HttpDomainErrorHandler:
    def apply[F[_]: Concurrent: Logger](routes: HttpRoutes[F]): HttpRoutes[F] =
      ErrorHandler().httpDomainErrorHandler(routes)
