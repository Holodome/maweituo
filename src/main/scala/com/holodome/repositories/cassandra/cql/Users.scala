package com.holodome.repositories.cassandra.cql

import com.holodome.domain.users.User
import com.outworkers.phantom.dsl._

abstract class Users extends Table[Users, User] {
  override def tableName: String = "users"

  object id        extends UUIDColumn with PartitionKey
  object name      extends StringColumn
  object email     extends StringColumn
  object password  extends StringColumn
  object salt      extends StringColumn
}
