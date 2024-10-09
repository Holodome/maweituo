package maweituo.logic.errors

import scala.util.control.NoStackTrace

import cats.Show
import cats.data.NonEmptyList
import cats.derived.derived
import cats.syntax.all.*

import maweituo.domain.ads.*
import maweituo.domain.ads.images.*
import maweituo.domain.ads.messages.*
import maweituo.domain.users.*
import maweituo.logic.search.SearchValidationError

enum DomainError extends NoStackTrace derives Show:

  override def getMessage(): String = this.show

  case UserModificationForbidden(violator: UserId)

  case InvalidUserId(userId: UserId)
  case NoUserWithName(username: Username)
  case NoUserWithEmail(email: Email)
  case UserNameInUse(username: Username)
  case UserEmailInUse(email: Email)
  case InvalidPassword(username: Username)

  case InvalidChatId(chatId: ChatId)
  case ChatAccessForbidden(chatId: ChatId)

  case InvalidImageId(imageId: ImageId)
  case InternalImageUnsync(reason: String)

  case InvalidAdId(id: AdId)
  case CannotCreateChatWithMyself(adId: AdId, user: UserId)
  case ChatAlreadyExists(adId: AdId, clientId: UserId)
  case AdModificationForbidden(adId: AdId, userId: UserId)

  case InvalidSearchParams(errors: NonEmptyList[SearchValidationError])
