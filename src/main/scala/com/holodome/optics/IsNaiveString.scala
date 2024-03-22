package com.holodome.optics

import com.holodome.ext.derevo.Derive
import monocle.Iso

trait IsNaiveString[A] {
  def _String: Iso[String, A]
}

object IsNaiveString {
  def apply[A: IsNaiveString]: IsNaiveString[A] = implicitly

  implicit def identityNaiveString: IsNaiveString[String] = new IsNaiveString[String] {
    override def _String: Iso[String, String] = Iso[String, String](identity)(identity)
  }
}

object naiveString extends Derive[IsNaiveString]
