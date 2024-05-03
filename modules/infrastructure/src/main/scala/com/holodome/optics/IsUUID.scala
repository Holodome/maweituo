package com.holodome.optics

import monocle.Iso

import java.util.UUID
import com.holodome.ext.derevo.Derive

trait IsUUID[A] {
  def _UUID: Iso[UUID, A]
}

object IsUUID {
  def apply[A: IsUUID]: IsUUID[A] = implicitly

  implicit val identityUUID: IsUUID[UUID] = new IsUUID[UUID] {
    override def _UUID: Iso[UUID, UUID] = Iso[UUID, UUID](identity)(identity)
  }
}

object uuidIso extends Derive[IsUUID]
