package maweituo.http.vars

import java.util.UUID

import scala.util.Try

import cats.syntax.all.*

import maweituo.domain.ads.images.ImageId
import maweituo.domain.ads.messages.ChatId
import maweituo.domain.ads.{AdId, AdTag}
import maweituo.domain.users.UserId

sealed class UUIDVar[A](f: UUID => A):
  def unapply(str: String): Option[A] =
    Try(UUID.fromString(str)).toOption.map(f)

object UserIdVar  extends UUIDVar(UserId.apply)
object AdIdVar    extends UUIDVar(AdId.apply)
object ChatIdVar  extends UUIDVar(ChatId.apply)
object ImageIdVar extends UUIDVar(ImageId.apply)

object TagVar:
  def unapply(str: String): Option[AdTag] =
    AdTag(str).some
