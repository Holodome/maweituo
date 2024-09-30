package maweituo.domain.errors

import scala.util.control.NoStackTrace

import maweituo.domain.ads.*
import maweituo.domain.ads.images.*
import maweituo.domain.ads.messages.*
import maweituo.domain.users.*

final case class UserModificationForbidden(violator: UserId) extends NoStackTrace

final case class InvalidUserId(userid: UserId)       extends NoStackTrace
final case class NoUserWithName(username: Username)  extends NoStackTrace
final case class NoUserWithEmail(email: Email)       extends NoStackTrace
final case class UserNameInUse(username: Username)   extends NoStackTrace
final case class UserEmailInUse(email: Email)        extends NoStackTrace
final case class InvalidPassword(username: Username) extends NoStackTrace

final case class InvalidChatId(chatId: ChatId)       extends NoStackTrace
final case class ChatAccessForbidden(chatId: ChatId) extends NoStackTrace

final case class InvalidImageId(imageId: ImageId)    extends NoStackTrace
final case class InternalImageUnsync(reason: String) extends NoStackTrace

final case class InvalidAdId(id: AdId)                                extends NoStackTrace
final case class CannotCreateChatWithMyself(adId: AdId, user: UserId) extends NoStackTrace
final case class ChatAlreadyExists(adId: AdId, clientId: UserId)      extends NoStackTrace
final case class AdModificationForbidden(adId: AdId, userId: UserId)  extends NoStackTrace
