package com.holodome.optics

import com.holodome.ext.derevo.Derive
import monocle.Iso

trait IsString[A] {
  def _String: Iso[String, A]
}

object IsString {
  def apply[A: IsString]: IsString[A] = implicitly

  implicit val identityString: IsString[String] = new IsString[String] {
    override def _String: Iso[String, String] = Iso[String, String](identity)(identity)
  }
}

object stringIso extends Derive[IsString]
