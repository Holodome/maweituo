package com.holodome.repositories.cassandra.cql

import com.holodome.repositories.models.User
import com.outworkers.phantom.dsl._

abstract class Users extends Table[Users, User] {
  override def tableName: String = "users"

  object name extends StringColumn with PartitionKey
  object email extends StringColumn
  object password extends StringColumn
  object salt extends StringColumn
  object createdAt extends DateTimeColumn
  object updatedAt extends DateTimeColumn
}
