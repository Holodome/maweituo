package maweituo.http.errors

import cats.data.{Kleisli, OptionT}
import cats.effect.Concurrent
import cats.syntax.all.*

import maweituo.domain.errors.DomainError
import maweituo.domain.users.UserId
import maweituo.http.dto.ErrorResponseDto

import org.http4s.circe.CirceEntityCodec.given
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Request, Response}
import org.typelevel.log4cats.Logger

private final case class ErrorHandler[F[_]: Concurrent: Logger]() extends Http4sDsl[F]:
  private def forbidden(error: String)  = Forbidden(ErrorResponseDto.make(error))
  private def conflict(error: String)   = Conflict(ErrorResponseDto.make(error))
  private def badRequest(error: String) = BadRequest(ErrorResponseDto.make(error))

  private def errorToResponse(error: DomainError): F[Response[F]] =
    error match
      case DomainError.UserModificationForbidden(violator) =>
        forbidden(f"user modification forbidden by user $violator")
      case DomainError.InvalidUserId(userId)       => badRequest(f"invalid user id $userId")
      case DomainError.NoUserWithName(username)    => badRequest(f"no user with name $username found")
      case DomainError.NoUserWithEmail(email)      => badRequest(f"no user with email $email found")
      case DomainError.UserNameInUse(username)     => conflict(f"name $username is aleady taken")
      case DomainError.UserEmailInUse(email)       => conflict(f"email $email is already taken")
      case DomainError.InvalidPassword(username)   => forbidden(f"invalid password for user $username")
      case DomainError.InvalidChatId(chatId)       => badRequest(f"invalid chat id $chatId")
      case DomainError.ChatAccessForbidden(chatId) => forbidden(f"access to chat $chatId forbidden")
      case DomainError.InvalidImageId(imageId)     => badRequest(f"invalid image id $imageId")
      case DomainError.InternalImageUnsync(reason) => InternalServerError()
      case DomainError.InvalidAdId(id)             => badRequest(f"invalid ad id $id")
      case DomainError.CannotCreateChatWithMyself(adId, user) =>
        conflict(f"can't create chat with yourself for user $user ad $adId")
      case DomainError.ChatAlreadyExists(adId, clientId) =>
        conflict(f"chat for ad $adId with user $clientId already exists")
      case DomainError.AdModificationForbidden(adId, userId) =>
        forbidden(f"ad $adId modification by user $userId forbidden")

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
