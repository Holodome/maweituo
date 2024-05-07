package com.holodome

import cats.Applicative
import cats.effect.std.Console
import cats.effect.{Async, Sync}
import cats.syntax.all._
import com.holodome.domain.Id
import com.holodome.domain.ads._
import com.holodome.domain.messages.{ChatId, MessageText, SendMessageRequest}
import com.holodome.domain.pagination.Pagination
import com.holodome.domain.users._
import com.holodome.modules.Services
import com.holodome.utils.EncodeRF
import eu.timepit.refined.auto._

import scala.util.Try

object ConsoleApi {

  def make[F[_]: Console: Async](services: Services[F]): ConsoleApi[F] =
    new ConsoleApi(services)

}

final class ConsoleApi[F[_]: Async] private (services: Services[F])(implicit C: Console[F]) {
  import commands._

  private var loggedUserId: Option[UserId] = None

  private def getCurrentUser: F[Option[UserId]] = Applicative[F].pure(loggedUserId)
  private def setCurrentUser(user: Option[UserId]): F[Unit] = Sync[F].delay({
    loggedUserId = user
  })

  def err[A](msg: String): F[A] = new RuntimeException(msg).raiseError[F, A]

  def promptInput[A](prompt: String, convert: String => A): F[A] = for {
    _ <- C.print(prompt)
    r <- C.readLine.map(convert)
  } yield r

  def promptInputF[A](prompt: String, convert: String => F[A]): F[A] = for {
    _ <- C.print(prompt)
    r <- C.readLine.flatMap(convert)
  } yield r

  private def parseCommand(n: Int): F[Command] = {
    def ok[A <: Command](a: A): F[Command] = Applicative[F].pure(a)
    n match {
      case 0  => ok(Exit())
      case 1  => ok(Register())
      case 2  => ok(GetUserInfo())
      case 3  => ok(GetAd())
      case 4  => ok(UploadAd())
      case 5  => ok(AddTag())
      case 6  => ok(MarkResolved())
      case 7  => ok(CreateChat())
      case 8  => ok(SendMessage())
      case 9  => ok(GetChatHistory())
      case 10 => ok(GetPersonalizedFeed())
      case 11 => ok(GetGlobalFeed())
      case 12 => ok(GetAllTags())
      case _  => err("Invalid command number")
    }
  }

  private def readCommand: F[Command] = for {
    n <- promptInputF[Int](
      "Enter command number: ",
      nStr =>
        Try(nStr.toInt).toOption match {
          case Some(n) => Applicative[F].pure(n)
          case None    => err("Invalid input")
        }
    )
    cmd <- parseCommand(n)
  } yield cmd

  private def login: F[Unit] = for {
    name     <- promptInput("Input name: ", Username.apply)
    password <- promptInput("Input password", Password.apply)
    userId   <- services.auth.login(name, password).map(_._2)
    _        <- setCurrentUser(userId.some)
    _        <- C.println(s"Logged in user $userId")
  } yield ()

  private def logout: F[Unit] =
    setCurrentUser(None) *> C.println("Logged out")

  private def register: F[Unit] = for {
    name     <- promptInput("Input name: ", Username.apply)
    email    <- promptInputF("Input email: ", EncodeRF[F, String, Email].encodeRF)
    password <- promptInput("Input password", Password.apply)
    req = RegisterRequest(name, email, password)
    _ <- services.users.create(req)
    _ <- C.println("Registered")
  } yield ()

  private def getUserInfo: F[Unit] = for {
    userId <- promptInputF("Input user id: ", Id.read[F, UserId])
    user   <- services.users.get(userId)
    _      <- C.println(s"user: $user")
  } yield ()

  private def getAd: F[Unit] = for {
    adId <- promptInputF("Input ad id: ", Id.read[F, AdId])
    ad   <- services.ads.get(adId)
    _    <- C.println(s"ad: $ad")
  } yield ()

  private def uploadAd(userId: UserId): F[Unit] = for {
    title <- promptInput("Input ad title: ", AdTitle.apply)
    adId  <- services.ads.create(userId, CreateAdRequest(title))
    _     <- C.println(s"Created ad $adId")
  } yield ()

  private def addTag(userId: UserId): F[Unit] = for {
    adId <- promptInputF("Input ad id: ", Id.read[F, AdId])
    tag  <- promptInput("Input tag: ", AdTag.apply)
    _    <- services.ads.addTag(adId, tag, userId)
    _    <- C.println("Added tag")
  } yield ()

  private def markResolved(userId: UserId): F[Unit] = for {
    adId   <- promptInputF("Input ad id: ", Id.read[F, AdId])
    client <- promptInputF("Input client id: ", Id.read[F, UserId])
    _      <- services.ads.markAsResolved(adId, userId, client)
    _      <- C.println("Resolved")
  } yield ()

  private def createChat(userId: UserId): F[Unit] = for {
    adId   <- promptInputF("Input ad id: ", Id.read[F, AdId])
    client <- promptInputF("Input client id: ", Id.read[F, UserId])
    chatId <- services.chats.create(adId, client)
    _      <- C.println(s"Created chat $chatId")
  } yield ()

  private def sendMessage(userId: UserId): F[Unit] = for {
    chatId <- promptInputF("Input chat id: ", Id.read[F, ChatId])
    msg    <- promptInput("Input message text: ", MessageText.apply)
    _      <- services.messages.send(chatId, userId, SendMessageRequest(msg))
    _      <- C.println("Message sent")
  } yield ()

  private def getChatHistory(userId: UserId): F[Unit] = for {
    chatId  <- promptInputF("Input chat id: ", Id.read[F, ChatId])
    history <- services.messages.history(chatId, userId)
    _       <- C.println(s"History: $history")
  } yield ()

  private def getGlobalFeed: F[Unit] = for {
    feed <- services.feed.getGlobal(Pagination(10, 0))
    _    <- C.println("Feed: $feed")
  } yield ()

  private def getPersonalizedFeed(userId: UserId): F[Unit] = for {
    feed <- services.feed.getPersonalized(userId, Pagination(10, 0))
    _    <- C.println("Feed: $feed")
  } yield ()

  private def getAllTags: F[Unit] = for {
    tags <- services.tags.all
    _    <- C.println(s"Tags: $tags")
  } yield ()

  private def executeCommand(cmd: Command): F[Unit] = {
    def requireUnlogged(action: => F[Unit]): F[Unit] =
      getCurrentUser flatMap {
        case Some(_) => err("Logout before you can do that")
        case None    => action
      }
    def requireLogin(action: UserId => F[Unit]): F[Unit] =
      getCurrentUser flatMap {
        case Some(u) => action(u)
        case None    => err("Login before you can do that")
      }
    cmd match {
      case Exit()                => err("Exit")
      case Login()               => requireUnlogged(login)
      case Logout()              => requireLogin(_ => logout)
      case Register()            => requireUnlogged(register)
      case GetUserInfo()         => getUserInfo
      case GetAd()               => getAd
      case UploadAd()            => requireLogin(uploadAd)
      case AddTag()              => requireLogin(addTag)
      case MarkResolved()        => requireLogin(markResolved)
      case CreateChat()          => requireLogin(createChat)
      case SendMessage()         => requireLogin(sendMessage)
      case GetChatHistory()      => requireLogin(getChatHistory)
      case GetPersonalizedFeed() => requireLogin(getPersonalizedFeed)
      case GetGlobalFeed()       => getGlobalFeed
      case GetAllTags()          => getAllTags
    }
  }

  def run: F[Unit] =
    (readCommand >>= executeCommand)
      .as(false)
      .recoverWith {
        case e if e.getMessage() === "Exit" => Applicative[F].pure(true)
        case e                              => C.println(s"Error: $e") *> Applicative[F].pure(false)
      }
      .flatMap {
        case true  => Applicative[F].unit
        case false => run
      }
}

private object commands {
  sealed trait Command
  final case class Exit()                extends Command
  final case class Register()            extends Command
  final case class GetUserInfo()         extends Command
  final case class GetAd()               extends Command
  final case class UploadAd()            extends Command
  final case class AddTag()              extends Command
  final case class MarkResolved()        extends Command
  final case class CreateChat()          extends Command
  final case class SendMessage()         extends Command
  final case class GetChatHistory()      extends Command
  final case class GetPersonalizedFeed() extends Command
  final case class GetGlobalFeed()       extends Command
  final case class GetAllTags()          extends Command
  final case class Login()               extends Command
  final case class Logout()              extends Command
}
