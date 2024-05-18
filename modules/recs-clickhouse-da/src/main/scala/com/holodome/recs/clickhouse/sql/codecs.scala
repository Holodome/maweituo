package com.holodome.recs.sql

import cats.data.NonEmptyList
import doobie.Meta

import java.util.UUID

object codecs {
  private object arrayMapper {
    import doobie.util.meta.Meta
    import doobie.util.{Get, Put}
    import doobie.enumerated.JdbcType

    def array[A](
        elemType: String,
        arrayType: String,
        arrayTypeT: String*
    ): Meta[Array[A]] = new Meta[Array[A]](
      Get.Advanced.one(
        JdbcType.Array,
        NonEmptyList(arrayType, arrayTypeT.toList),
        (r, n) => {
          val a = r.getArray(n)
          (if (a == null) null else a.getArray).asInstanceOf[Array[A]]
        }
      ),
      Put.Advanced.one(
        JdbcType.Array,
        NonEmptyList(arrayType, arrayTypeT.toList),
        (ps, n, a) => {
          val conn = ps.getConnection
          val arr  = conn.createArrayOf(elemType, a.asInstanceOf[Array[AnyRef]])
          ps.setArray(n, arr)
        },
        (rs, n, a) => {
          val stmt = rs.getStatement
          val conn = stmt.getConnection
          val arr  = conn.createArrayOf(elemType, a.asInstanceOf[Array[AnyRef]])
          rs.updateArray(n, arr)
        }
      )
    )
  }

  implicit val uuidMeta: Meta[UUID] =
    Meta[String].imap[UUID](UUID.fromString)(_.toString)

  implicit val unliftedIntegerArrayType: Meta[Array[Int]] =
    arrayMapper.array[Int]("Int32", "Array(Int32)")

  implicit val unliftedLongArrayType: Meta[Array[Long]] =
    arrayMapper.array[Long]("Int64", "Array(Int64)")

  implicit val unliftedFloatArrayType: Meta[Array[Float]] =
    arrayMapper.array[Float]("Float32", "Array(Float32)")

  implicit val unliftedDoubleArrayType: Meta[Array[Double]] =
    arrayMapper.array[Double]("Float64", "Array(Float64)")

  implicit val unliftedStringArrayType: Meta[Array[String]] =
    arrayMapper.array[String]("String", "Array(String)")

  implicit val unliftedUUIDArrayType: Meta[Array[UUID]] =
    arrayMapper.array[UUID]("UUID", "Array(UUID)")
}
