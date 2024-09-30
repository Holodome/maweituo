package maweituo.tests.generators

import java.time.Instant
import java.util.UUID

import maweituo.auth.PasswordHashing
import maweituo.domain.ads.images.{ImageContentsStream, ImageId, ImageUrl, MediaType}
import maweituo.domain.ads.messages.{ChatId, MessageText, SendMessageRequest}
import maweituo.domain.ads.{AdId, AdTag, AdTitle, Advertisement, CreateAdRequest}
import maweituo.domain.users.*
import maweituo.infrastructure.OBSId

import org.scalacheck.Gen

val nonEmptyStringGen: Gen[String] =
  Gen
    .chooseNum(10, 20)
    .flatMap(Gen.buildableOfN[String, Char](_, Gen.alphaChar))

def nesGen[A](f: String => A): Gen[A] =
  nonEmptyStringGen map f

val byteArrayGen: Gen[Array[Byte]] =
  Gen
    .chooseNum(32, 64)
    .flatMap(Gen.listOfN(_, Gen.chooseNum(0, 255).map(_.byteValue)).map(_.toArray))

val bigByteArrayGen: Gen[Array[Byte]] =
  Gen.listOfN(5 * 1024 * 1024 + 1, Gen.chooseNum(0, 255).map(_.byteValue)).map(_.toArray)

def idGen[A](f: UUID => A): Gen[A] = Gen.uuid map f

val userIdGen: Gen[UserId] = idGen(UserId.apply)

val usernameGen: Gen[Username] =
  nesGen(Username.apply)

val passwordGen: Gen[Password] =
  nesGen(Password.apply)

val saltGen: Gen[PasswordSalt] =
  nesGen(PasswordSalt.apply)

val emailGen: Gen[Email] =
  for
    prefix  <- nonEmptyStringGen
    postfix <- nonEmptyStringGen
    domain <- Gen
      .chooseNum(2, 4)
      .flatMap(Gen.buildableOfN[String, Char](_, Gen.alphaChar))
  yield Email(prefix + "@" + postfix + "." + domain)

val registerGen: Gen[RegisterRequest] =
  for
    name     <- usernameGen
    email    <- emailGen
    password <- passwordGen
  yield RegisterRequest(name, email, password)

def updateUserGen(id: UserId): Gen[UpdateUserRequest] =
  for
    name     <- Gen.option(usernameGen)
    email    <- Gen.option(emailGen)
    password <- Gen.option(passwordGen)
  yield UpdateUserRequest(id, name, email, password)

val adTitleGen: Gen[AdTitle] =
  nesGen(AdTitle.apply)

val createAdRequestGen: Gen[CreateAdRequest] =
  for
    title <- adTitleGen
  yield CreateAdRequest(title)

val adIdGen: Gen[AdId] =
  idGen(AdId.apply)

val imageIdGen: Gen[ImageId] =
  idGen(ImageId.apply)

val msgTextGen: Gen[MessageText] =
  nesGen(MessageText.apply)

val sendMessageRequestGen: Gen[SendMessageRequest] =
  for
    msg <- msgTextGen
  yield SendMessageRequest(msg)

def imageContentsGen[F[_]]: Gen[ImageContentsStream[F]] =
  byteArrayGen.map(arr =>
    ImageContentsStream[F](
      fs2.Stream.emits(arr).covary[F],
      MediaType("image", "jpeg"),
      arr.length
    )
  )

val objectIdGen: Gen[OBSId] =
  nesGen(OBSId.apply)

val userGen: Gen[User] =
  for
    id       <- userIdGen
    name     <- usernameGen
    email    <- emailGen
    password <- passwordGen
    salt     <- saltGen
    hashedPassword = PasswordHashing.hashSaltPassword(password, salt)
  yield User(id, name, email, hashedPassword, salt)

val adGen: Gen[Advertisement] =
  for
    id     <- adIdGen
    title  <- adTitleGen
    author <- userIdGen
  yield Advertisement(id, author, title, resolved = false)

val chatIdGen: Gen[ChatId] =
  idGen(ChatId.apply)

private val minInstantSeconds: Long = 0L
private val maxInstantSeconds: Long = 1893456000L

val instantGen: Gen[Instant] =
  for
    seconds        <- Gen.choose(minInstantSeconds, maxInstantSeconds)
    nanoAdjustment <- Gen.choose(0L, 999_999_999L)
  yield Instant.ofEpochSecond(seconds, nanoAdjustment)

val imageUrlGen: Gen[ImageUrl] = nesGen(s => ImageUrl(s))

val adTagGen: Gen[AdTag] = nesGen(AdTag.apply)

val loginRequestGen: Gen[LoginRequest] =
  for
    name     <- usernameGen
    password <- passwordGen
  yield LoginRequest(name, password)

val registerRequestGen: Gen[RegisterRequest] =
  for
    name     <- usernameGen
    email    <- emailGen
    password <- passwordGen
  yield RegisterRequest(name, email, password)
