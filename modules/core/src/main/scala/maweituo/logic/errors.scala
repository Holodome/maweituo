package maweituo
package logic

import scala.util.control.NoStackTrace

import maweituo.domain.all.*
import maweituo.logic.search.SearchValidationError

object errors:
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
