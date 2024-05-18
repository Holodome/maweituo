package com.holodome.cassandra.cql

import com.datastax.oss.driver.api.core.`type`.DataType
import com.holodome.domain.ads._
import com.holodome.domain.images.{ImageId, ImageUrl}
import com.holodome.domain.messages.{ChatId, MessageText}
import com.holodome.domain.users._
import com.holodome.optics.{IsUUID}
import com.holodome.utils.EncodeR
import com.ringcentral.cassandra4io.cql.{CassandraTypeMapper, Reads}

package object codecs {
  import com.ringcentral.cassandra4io.cql.Reads._

  private def encodeThrow[T, A](t: T)(implicit E: EncodeR[T, A]): A =
    E.encodeRF(t) match {
      case Left(e)  => throw e
      case Right(a) => a
    }

  private def readThrow[T, A](implicit R: Reads[T], E: EncodeR[T, A]): Reads[A] =
    R.map(encodeThrow[T, A])

  implicit val userIdReads: Reads[UserId]     = uuidReads.map(UserId.apply)
  implicit val usernameReads: Reads[Username] = readThrow[String, Username]
  implicit val emailReads: Reads[Email]       = readThrow[String, Email]
  implicit val passwordReads: Reads[HashedSaltedPassword] =
    stringReads.map(HashedSaltedPassword.apply)
  implicit val saltReads: Reads[PasswordSalt] = readThrow[String, PasswordSalt]

  implicit val adIdReads: Reads[AdId]       = uuidReads.map(AdId.apply)
  implicit val adTitleReads: Reads[AdTitle] = readThrow[String, AdTitle]
  implicit val adTagReads: Reads[AdTag]     = readThrow[String, AdTag]

  implicit val imageIdReads: Reads[ImageId]   = uuidReads.map(ImageId.apply)
  implicit val imageUrlReads: Reads[ImageUrl] = stringReads.map(ImageUrl.apply)

  implicit val chatIdReads: Reads[ChatId]           = uuidReads.map(ChatId.apply)
  implicit val messageTextReads: Reads[MessageText] = readThrow[String, MessageText]

  implicit def cassandraTypeMapperUuid[T: IsUUID]: CassandraTypeMapper[T] =
    new CassandraTypeMapper[T] {
      type Cassandra = java.util.UUID

      def classType: Class[Cassandra] = classOf[Cassandra]

      def toCassandra(in: T, dataType: DataType): Cassandra =
        IsUUID[T]._UUID.reverseGet(in)

      def fromCassandra(in: Cassandra, dataType: DataType): T =
        IsUUID[T]._UUID.get(in)
    }

  private def cassandraTypeMapperString[T](
      f: T => String
  )(implicit E: EncodeR[String, T]): CassandraTypeMapper[T] =
    new CassandraTypeMapper[T] {
      type Cassandra = String

      def classType: Class[Cassandra] = classOf[Cassandra]

      def toCassandra(in: T, dataType: DataType): Cassandra = f(in)

      def fromCassandra(in: Cassandra, dataType: DataType): T = encodeThrow[String, T](in)
    }

  implicit def adTagCassandraTypeMapping: CassandraTypeMapper[AdTag] =
    cassandraTypeMapperString(_.str)

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
