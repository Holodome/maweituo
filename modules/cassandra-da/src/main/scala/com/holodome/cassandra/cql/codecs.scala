package com.holodome.cql

import com.datastax.oss.driver.api.core.`type`.DataType
import com.holodome.domain.ads._
import com.holodome.domain.images.{ImageId, ImageUrl}
import com.holodome.domain.messages.{ChatId, MessageText}
import com.holodome.domain.users._
import com.holodome.optics.{IsString, IsUUID}
import com.holodome.utils.EncodeR
import com.ringcentral.cassandra4io.cql.{CassandraTypeMapper, Reads}

object codecs {
  import com.ringcentral.cassandra4io.cql.Reads._

  private def encodeThrow[T, A](t: T)(implicit E: EncodeR[T, A]): A =
    E.encodeR(t) match {
      case Left(e)  => throw e
      case Right(a) => a
    }

  implicit val userIdReads: Reads[UserId]     = uuidReads.map(UserId.apply)
  implicit val usernameReads: Reads[Username] = stringReads.map(Username.apply)
  implicit val emailReads: Reads[Email] =
    stringReads.map(e => Email(encodeThrow[String, EmailT](e)))
  implicit val passwordReads: Reads[HashedSaltedPassword] =
    stringReads.map(HashedSaltedPassword.apply)
  implicit val saltReads: Reads[PasswordSalt] = stringReads.map(PasswordSalt.apply)

  implicit val adIdReads: Reads[AdId]       = uuidReads.map(AdId.apply)
  implicit val adTitleReads: Reads[AdTitle] = stringReads.map(AdTitle.apply)
  implicit val adTagReads: Reads[AdTag]     = stringReads.map(AdTag.apply)

  implicit val imageIdReads: Reads[ImageId]   = uuidReads.map(ImageId.apply)
  implicit val imageUrlReads: Reads[ImageUrl] = stringReads.map(ImageUrl.apply)

  implicit val chatIdReads: Reads[ChatId]           = uuidReads.map(ChatId.apply)
  implicit val messageTextReads: Reads[MessageText] = stringReads.map(MessageText.apply)

  implicit def cassandraTypeMapperUuid[T: IsUUID]: CassandraTypeMapper[T] =
    new CassandraTypeMapper[T] {
      type Cassandra = java.util.UUID

      def classType: Class[Cassandra] = classOf[Cassandra]

      def toCassandra(in: T, dataType: DataType): Cassandra =
        IsUUID[T]._UUID.reverseGet(in)

      def fromCassandra(in: Cassandra, dataType: DataType): T =
        IsUUID[T]._UUID.get(in)
    }

  implicit def cassandraTypeMapperString[T: IsString]: CassandraTypeMapper[T] =
    new CassandraTypeMapper[T] {
      type Cassandra = String

      def classType: Class[Cassandra] = classOf[Cassandra]

      def toCassandra(in: T, dataType: DataType): Cassandra = IsString[T]._String.reverseGet(in)

      def fromCassandra(in: Cassandra, dataType: DataType): T = IsString[T]._String.get(in)
    }

  private case class SerializedAd(
      id: AdId,
      authorId: UserId,
      title: AdTitle,
      tags: Option[Set[AdTag]],
      images: Option[Set[ImageId]],
      chats: Option[Set[ChatId]],
      resolved: Boolean
  ) {
    def toDomain: Advertisement =
      Advertisement(
        id,
        authorId,
        title,
        tags.getOrElse(Set()),
        images.getOrElse(Set()),
        chats.getOrElse(Set()),
        resolved
      )
  }

  implicit val adReads: Reads[Advertisement] = implicitly[Reads[SerializedAd]].map(_.toDomain)
}
