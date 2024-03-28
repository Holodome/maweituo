package com.holodome.ext.cassandra4io

import com.datastax.oss.driver.api.core.`type`.DataType
import com.holodome.optics.IsUUID
import com.ringcentral.cassandra4io.cql.CassandraTypeMapper

object typeMapperUuid {

  implicit def cassandraTypeMapperUuid[T: IsUUID]: CassandraTypeMapper[T] =
    new CassandraTypeMapper[T] {
      type Cassandra = java.util.UUID

      def classType: Class[Cassandra] = classOf[Cassandra]

      def toCassandra(in: T, dataType: DataType): Cassandra =
        IsUUID[T]._UUID.reverseGet(in)

      def fromCassandra(in: Cassandra, dataType: DataType): T =
        IsUUID[T]._UUID.get(in)
    }
}
