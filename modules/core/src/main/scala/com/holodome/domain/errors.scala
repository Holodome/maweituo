package com.holodome.domain

import com.holodome.domain.ads.AdId
import com.holodome.domain.users.{Email, Username}

import scala.util.control.NoStackTrace

object errors {
  sealed trait ApplicationError extends NoStackTrace

  case class InvalidAccess() extends ApplicationError

  case class InvalidUserId() extends ApplicationError
  case class NoUserFound(username: Username) extends ApplicationError
  case class UserNameInUse(username: Username) extends ApplicationError
  case class UserEmailInUse(email: Email) extends ApplicationError
  case class InvalidPassword(username: Username) extends ApplicationError

  case class InvalidChatId() extends ApplicationError
  case class ChatAccessForbidden() extends ApplicationError

  case class InvalidImageId() extends ApplicationError
  case class InternalImageUnsync() extends ApplicationError

  final case class InvalidAdId(id: AdId) extends ApplicationError
  final case class CannotCreateChatWithMyself() extends ApplicationError
  final case class ChatAlreadyExists() extends ApplicationError
  final case class NotAnAuthor() extends ApplicationError

  final case class DatabaseEncodingError() extends ApplicationError

}
