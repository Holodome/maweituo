package com.holodome.ext.cassandra4io

import com.datastax.oss.driver.api.core.`type`.DataType
import com.holodome.optics.{IsString, IsUUID}
import com.ringcentral.cassandra4io.cql.CassandraTypeMapper

object typeMappers {

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
}
