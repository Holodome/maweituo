package com.holodome.config

import com.comcast.ip4s.Host
import eu.timepit.refined.types.string.NonEmptyString
import com.comcast.ip4s.Port

case class CassandraConfig(host: Host, port: Port, datacenter: NonEmptyString, keyspace: String)
