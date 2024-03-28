package com.holodome.optics

import com.datastax.oss.driver.api.core.cql.Row
import com.ringcentral.cassandra4io.cql.Reads
import derevo.{Derivation, NewTypeDerivation}
import magnolia.CaseClass

object cassandraReads extends Derivation[Reads] with NewTypeDerivation[Reads] {
  type Typeclass[T] = Reads[T]

  def combine[T](ctx: CaseClass[Reads, T]): Reads[T] = new Typeclass[T] {
    override def readNullable(row: Row, index: Int): T =
      ctx.construct { param =>
        param.typeclass.readNullable(row, index)
      }
  }
}
