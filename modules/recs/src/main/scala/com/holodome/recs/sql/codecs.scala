package com.holodome.recs.sql

import doobie.Meta

import java.util.UUID

object codecs {
  implicit val uuidMeta: Meta[UUID] =
    Meta[String].imap[UUID](UUID.fromString)(_.toString)
}
