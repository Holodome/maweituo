package com.holodome.recs.sql

import doobie.Meta
import doobie.util.invariant.{NullableCellRead, NullableCellUpdate}

import java.util.UUID
import scala.reflect.ClassTag

object codecs {
  implicit val uuidMeta: Meta[UUID] =
    Meta[String].imap[UUID](UUID.fromString)(_.toString)

  private def boxedPair[A >: Null <: AnyRef: ClassTag](
      elemType: String,
      arrayType: String,
      arrayTypeT: String*
  ): (Meta[Array[A]], Meta[Array[Option[A]]]) = {
    val raw = Meta.Advanced.array[A](elemType, arrayType, arrayTypeT: _*)
    // Ensure `a`, which may be null, which is ok, contains no null elements.
    def checkNull[B >: Null](a: Array[B], e: Exception): Array[B] =
      if (a == null) null else if (a.contains(null)) throw e else a
    (
      raw.timap(checkNull(_, NullableCellRead))(checkNull(_, NullableCellUpdate)),
      raw.timap[Array[Option[A]]](_.map(Option(_)))(_.map(_.orNull).toArray)
    )
  }

  private val boxedPairBoolean                                                = boxedPair[java.lang.Boolean]("bit", "_bit")
  implicit val unliftedBooleanArrayType: Meta[Array[java.lang.Boolean]]       = boxedPairBoolean._1
  implicit val liftedBooleanArrayType: Meta[Array[Option[java.lang.Boolean]]] = boxedPairBoolean._2

  private val boxedPairInteger                                                = boxedPair[java.lang.Integer]("int4", "_int4")
  implicit val unliftedIntegerArrayType: Meta[Array[java.lang.Integer]]       = boxedPairInteger._1
  implicit val liftedIntegerArrayType: Meta[Array[Option[java.lang.Integer]]] = boxedPairInteger._2

  private val boxedPairLong                                             = boxedPair[java.lang.Long]("int8", "_int8")
  implicit val unliftedLongArrayType: Meta[Array[java.lang.Long]]       = boxedPairLong._1
  implicit val liftedLongArrayType: Meta[Array[Option[java.lang.Long]]] = boxedPairLong._2

  private val boxedPairFloat                                              = boxedPair[java.lang.Float]("float4", "_float4")
  implicit val unliftedFloatArrayType: Meta[Array[java.lang.Float]]       = boxedPairFloat._1
  implicit val liftedFloatArrayType: Meta[Array[Option[java.lang.Float]]] = boxedPairFloat._2

  private val boxedPairDouble                                               = boxedPair[java.lang.Double]("float8", "_float8")
  implicit val unliftedDoubleArrayType: Meta[Array[java.lang.Double]]       = boxedPairDouble._1
  implicit val liftedDoubleArrayType: Meta[Array[Option[java.lang.Double]]] = boxedPairDouble._2

  private val boxedPairString =
    boxedPair[java.lang.String]("varchar", "_varchar", "_char", "_text", "_bpchar")
  implicit val unliftedStringArrayType: Meta[Array[java.lang.String]]       = boxedPairString._1
  implicit val liftedStringArrayType: Meta[Array[Option[java.lang.String]]] = boxedPairString._2

  private val boxedPairUUID                                             = boxedPair[java.util.UUID]("uuid", "_uuid")
  implicit val unliftedUUIDArrayType: Meta[Array[java.util.UUID]]       = boxedPairUUID._1
  implicit val liftedUUIDArrayType: Meta[Array[Option[java.util.UUID]]] = boxedPairUUID._2
}
