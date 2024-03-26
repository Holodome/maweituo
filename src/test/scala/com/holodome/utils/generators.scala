package com.holodome.utils

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

  def userIdGen: Gen[UserId] = idGen(UserId.apply)

  def usernameGen: Gen[Username] =
    nesGen(Username.apply)

  def passwordGen: Gen[Password] =
    nesGen(Password.apply)

  def emailGen: Gen[Email] =
    for {
      prefix  <- nonEmptyStringGen
      postfix <- nonEmptyStringGen
    } yield Email(prefix + "@" + postfix)

  def registerGen: Gen[RegisterRequest] =
    for {
      name     <- usernameGen
      email    <- emailGen
      password <- passwordGen
    } yield RegisterRequest(name, email, password)

  def updateUserGen(id: UserId): Gen[UpdateUserRequest] =
    for {
      name     <- Gen.option(usernameGen)
      email    <- Gen.option(emailGen)
      password <- Gen.option(passwordGen)
    } yield UpdateUserRequest(id, name, email, password)
}
