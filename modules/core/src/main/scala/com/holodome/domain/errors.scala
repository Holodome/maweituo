package com.holodome.domain

import scala.util.control.NoStackTrace

import com.holodome.domain.ads.*
import com.holodome.domain.images.*
import com.holodome.domain.messages.*
import com.holodome.domain.users.*

package object errors:

  sealed trait ApplicationError extends NoStackTrace

  final case class InvalidAccess(reason: String) extends ApplicationError

  final case class InvalidUserId(userid: UserId)       extends ApplicationError
  final case class NoUserFound(username: Username)     extends ApplicationError
  final case class InvalidEmail(email: Email)          extends ApplicationError
  final case class UserNameInUse(username: Username)   extends ApplicationError
  final case class UserEmailInUse(email: Email)        extends ApplicationError
  final case class InvalidPassword(username: Username) extends ApplicationError

  final case class InvalidChatId(chatId: ChatId)       extends ApplicationError
  final case class ChatAccessForbidden(chatId: ChatId) extends ApplicationError

  final case class InvalidImageId(imageId: ImageId)    extends ApplicationError
  final case class InternalImageUnsync(reason: String) extends ApplicationError

  final case class InvalidAdId(id: AdId)                                extends ApplicationError
  final case class CannotCreateChatWithMyself(adId: AdId, user: UserId) extends ApplicationError
  final case class ChatAlreadyExists(adId: AdId, clientId: UserId)      extends ApplicationError
  final case class NotAnAuthor(adId: AdId, userId: UserId)              extends ApplicationError

  final case class DatabaseEncodingError(reason: String) extends ApplicationError
  final case class FeedError(reason: String)             extends ApplicationError
