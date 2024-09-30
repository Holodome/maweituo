package maweituo.http.vars

import java.util.UUID

import scala.util.Try

import maweituo.domain.ads.{ AdId, AdTag }
import maweituo.domain.images.ImageId
import maweituo.domain.messages.ChatId
import maweituo.domain.users.UserId

import cats.syntax.all.*

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
