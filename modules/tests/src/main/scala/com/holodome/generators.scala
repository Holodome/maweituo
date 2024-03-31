package com.holodome

import com.holodome.auth.PasswordHashing
import com.holodome.domain.users._
import com.holodome.domain.ads._
import com.holodome.domain.images._
import com.holodome.domain.messages._
import com.holodome.infrastructure.ObjectStorage.ObjectId
import org.scalacheck.Gen

import java.time.Instant
import java.util.UUID

object generators {
  def nonEmptyStringGen: Gen[String] =
    Gen
      .chooseNum(21, 40)
      .flatMap(Gen.buildableOfN[String, Char](_, Gen.alphaChar))

  def nesGen[A](f: String => A): Gen[A] =
    nonEmptyStringGen map f

  def byteArrayGen: Gen[Array[Byte]] =
    Gen
      .chooseNum(32, 64)
      .flatMap(Gen.listOfN(_, Gen.chooseNum(0, 255).map(_.byteValue)).map(_.toArray))

  def bigByteArrayGen: Gen[Array[Byte]] =
    Gen.listOfN(5 * 1024 * 1024 + 1, Gen.chooseNum(0, 255).map(_.byteValue)).map(_.toArray)

  def idGen[A](f: UUID => A): Gen[A] = Gen.uuid map f

  def userIdGen: Gen[UserId] = idGen(UserId.apply)

  def usernameGen: Gen[Username] =
    nesGen(Username.apply)

  def passwordGen: Gen[Password] =
    nesGen(Password.apply)

  def saltGen: Gen[PasswordSalt] =
    nesGen(PasswordSalt.apply)

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

  def adTitleGen: Gen[AdTitle] =
    nesGen(AdTitle.apply)

  def createAdRequestGen: Gen[CreateAdRequest] =
    for {
      title <- adTitleGen
    } yield CreateAdRequest(title)

  def adIdGen: Gen[AdId] =
    idGen(AdId.apply)

  def imageIdGen: Gen[ImageId] =
    idGen(ImageId.apply)

  def msgTextGen: Gen[MessageText] =
    nesGen(MessageText.apply)

  def sendMessageRequestGen: Gen[SendMessageRequest] =
    for {
      msg <- msgTextGen
    } yield SendMessageRequest(msg)

  def imageContentsGen: Gen[ImageContents] =
    byteArrayGen.map(ImageContents.apply)

  def objectIdGen: Gen[ObjectId] =
    nesGen(ObjectId.apply)

  def userGen: Gen[User] =
    for {
      id       <- userIdGen
      name     <- usernameGen
      email    <- emailGen
      password <- passwordGen
      salt     <- saltGen
      hashedPassword = PasswordHashing.hashSaltPassword(password, salt)
    } yield User(id, name, email, hashedPassword, salt)

  def createAdGen: Gen[Advertisement] =
    for {
      id     <- adIdGen
      title  <- adTitleGen
      author <- userIdGen
    } yield Advertisement(id, title, Set(), Set(), Set(), author)

  def chatIdGen: Gen[ChatId] =
    idGen(ChatId.apply)

  private val minInstantSeconds: Long = 0L
  private val maxInstantSeconds: Long = 1893456000L

  def instantGen: Gen[Instant] = for {
    seconds        <- Gen.choose(minInstantSeconds, maxInstantSeconds)
    nanoAdjustment <- Gen.choose(0L, 999_999_999L)
  } yield Instant.ofEpochSecond(seconds, nanoAdjustment)

  def imageUrlGen: Gen[ImageUrl] = nesGen(ImageUrl.apply)
}