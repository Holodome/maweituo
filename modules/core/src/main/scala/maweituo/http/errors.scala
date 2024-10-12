package maweituo
package http

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.syntax.all.*

import maweituo.domain.users.UserId
import maweituo.http.dto.ErrorResponseDto
import maweituo.logic.errors.DomainError

import sttp.model.StatusCode

object errors:
  private def notFound(error: String)            = StatusCode.NotFound            -> ErrorResponseDto.make(error)
  private def forbidden(error: String)           = StatusCode.Forbidden           -> ErrorResponseDto.make(error)
  private def conflict(error: String)            = StatusCode.Conflict            -> ErrorResponseDto.make(error)
  private def internalServerError(error: String) = StatusCode.InternalServerError -> ErrorResponseDto.make(error)

  def domainErrorToResponseData(error: DomainError): (StatusCode, ErrorResponseDto) =
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
      case DomainError.InternalImageUnsync(reason) => internalServerError(reason)
      case DomainError.InvalidAdId(id)             => notFound(s"invalid ad id $id")
      case DomainError.CannotCreateChatWithMyself(adId, user) =>
        conflict(s"can't create chat with yourself for user $user ad $adId")
      case DomainError.ChatAlreadyExists(adId, clientId) =>
        conflict(s"chat for ad $adId with user $clientId already exists")
      case DomainError.AdModificationForbidden(adId, userId) =>
        forbidden(s"ad $adId modification by user $userId forbidden")
      case DomainError.InvalidSearchParams(errors) => StatusCode.BadRequest -> ErrorResponseDto(errors.map(_.show))

  extension [F[_]: MonadThrow, A](f: F[A])
    def toOut: F[Either[(StatusCode, ErrorResponseDto), A]] =
      f.map(t =>
        t.asRight[(StatusCode, ErrorResponseDto)]
      ).recoverWith { case e: DomainError =>
        val (status, data) = domainErrorToResponseData(e)
        (status, data).asLeft[A].pure[F]
      }
