package com.holodome

import com.holodome.domain.users._
import org.scalacheck.Gen

import java.util.UUID

object generators {
  def nonEmptyStringGen: Gen[String] =
    Gen
      .chooseNum(21, 40)
      .flatMap(Gen.buildableOfN[String, Char](_, Gen.alphaChar))

  def nesGen[A](f: String => A): Gen[A] =
    nonEmptyStringGen map f

  def idGen[A](f: UUID => A): Gen[A] = Gen.uuid map f

  def usernameGen: Gen[Username] =
    nesGen(Username.apply)

  def passwordGen: Gen[Password] =
    nesGen(Password.apply)
}
