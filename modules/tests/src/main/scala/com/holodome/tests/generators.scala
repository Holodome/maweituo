package com.holodome.tests

import com.holodome.auth.PasswordHashing
import com.holodome.domain.ads._
import com.holodome.domain.images._
import com.holodome.domain.messages._
import com.holodome.domain.users._
import com.holodome.infrastructure.ObjectStorage.OBSId
import eu.timepit.refined.api.Refined
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

  def emailGen: Gen[Email] = for {
    prefix  <- nonEmptyStringGen
    postfix <- nonEmptyStringGen
    domain <- Gen
      .chooseNum(2, 4)
      .flatMap(Gen.buildableOfN[String, Char](_, Gen.alphaChar))
  } yield Email(Refined.unsafeApply(prefix + "@" + postfix + "." + domain))

  def registerGen: Gen[RegisterRequest] = for {
    name     <- usernameGen
    email    <- emailGen
    password <- passwordGen
  } yield RegisterRequest(name, email, password)

  def updateUserGen(id: UserId): Gen[UpdateUserRequest] = for {
    name     <- Gen.option(usernameGen)
    email    <- Gen.option(emailGen)
    password <- Gen.option(passwordGen)
  } yield UpdateUserRequest(id, name, email, password)

  def adTitleGen: Gen[AdTitle] =
    nesGen(AdTitle.apply)

  def createAdRequestGen: Gen[CreateAdRequest] = for {
    title <- adTitleGen
  } yield CreateAdRequest(title)

  def adIdGen: Gen[AdId] =
    idGen(AdId.apply)

  def imageIdGen: Gen[ImageId] =
    idGen(ImageId.apply)

  def msgTextGen: Gen[MessageText] =
    nesGen(MessageText.apply)

  def sendMessageRequestGen: Gen[SendMessageRequest] = for {
    msg <- msgTextGen
  } yield SendMessageRequest(msg)

  def imageContentsGen[F[_]]: Gen[ImageContentsStream[F]] =
    byteArrayGen.map(arr =>
      ImageContentsStream[F](
        fs2.Stream.emits(arr).covary[F],
        MediaType("image", "jpeg"),
        arr.length
      )
    )

  def objectIdGen: Gen[OBSId] =
    nesGen(OBSId.apply)

  def userGen: Gen[User] = for {
    id       <- userIdGen
    name     <- usernameGen
    email    <- emailGen
    password <- passwordGen
    salt     <- saltGen
    hashedPassword = PasswordHashing.hashSaltPassword(password, salt)
  } yield User(id, name, email, hashedPassword, salt)

  def adGen: Gen[Advertisement] = for {
    id     <- adIdGen
    title  <- adTitleGen
    author <- userIdGen
  } yield Advertisement(id, author, title, Set(), Set(), Set(), resolved = false)

  def chatIdGen: Gen[ChatId] =
    idGen(ChatId.apply)

  private val minInstantSeconds: Long = 0L
  private val maxInstantSeconds: Long = 1893456000L

  def instantGen: Gen[Instant] = for {
    seconds        <- Gen.choose(minInstantSeconds, maxInstantSeconds)
    nanoAdjustment <- Gen.choose(0L, 999_999_999L)
  } yield Instant.ofEpochSecond(seconds, nanoAdjustment)

  def imageUrlGen: Gen[ImageUrl] = nesGen(ImageUrl.apply)

  def adTagGen: Gen[AdTag] = nesGen(AdTag.apply)

  def loginRequestGen: Gen[LoginRequest] = for {
    name     <- usernameGen
    password <- passwordGen
  } yield LoginRequest(name, password)

  def registerRequestGen: Gen[RegisterRequest] = for {
    name     <- usernameGen
    email    <- emailGen
    password <- passwordGen
  } yield RegisterRequest(name, email, password)
}
