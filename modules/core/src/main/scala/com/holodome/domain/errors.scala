package com.holodome.domain

import com.holodome.domain.ads._
import com.holodome.domain.images._
import com.holodome.domain.messages._
import com.holodome.domain.users._

import scala.util.control.NoStackTrace

package object errors {

  sealed trait ApplicationError extends NoStackTrace

  final case class InvalidAccess(reason: String) extends ApplicationError

  final case class InvalidUserId(userid: UserId)       extends ApplicationError
  final case class NoUserFound(username: Username)     extends ApplicationError
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

}
