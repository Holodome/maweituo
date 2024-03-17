package com.holodome.repositories.cassandra.cql

import com.outworkers.phantom.dsl._

class UsersDatabase(override val connector: CassandraConnection)
    extends Database[UsersDatabase](connector) {
  object users extends Users with Connector
}
