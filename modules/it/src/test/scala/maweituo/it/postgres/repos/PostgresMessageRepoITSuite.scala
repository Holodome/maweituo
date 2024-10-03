package maweituo.it.postgres.repos

import cats.effect.*

import maweituo.domain.ads.repos.AdRepo
import maweituo.domain.users.repos.UserRepo
import maweituo.postgres.ads.repos.PostgresAdRepo
import maweituo.postgres.repos.users.PostgresUserRepo
import maweituo.tests.containers.*
import maweituo.tests.generators.{adGen, userGen}
import maweituo.tests.utils.given
import maweituo.tests.{ResourceSuite, WeaverLogAdapter}

import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.*
import weaver.scalacheck.Checkers
import maweituo.domain.ads.repos.ChatRepo
import maweituo.postgres.ads.repos.PostgresChatRepo
import maweituo.domain.ads.messages.Chat
import maweituo.tests.generators.chatIdGen
import maweituo.postgres.ads.repos.PostgresMessageRepo
import maweituo.domain.ads.repos.MessageRepo
import maweituo.domain.users.UserId
import maweituo.domain.ads.messages.ChatId
import maweituo.tests.generators.msgTextGen
import maweituo.tests.generators.instantGen
import maweituo.domain.ads.messages.Message

object PostgresMessageRepoITSuite extends ResourceSuite:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] =
    given Logger[IO] = NoOpLogger[IO]
    makePostgresResource[IO]

  private def msgTest(name: String)(fn: (UserRepo[IO], AdRepo[IO], ChatRepo[IO], MessageRepo[IO]) => F[Expectations]) =
    test(name) { (postgres, log) =>
      given Logger[IO] = new WeaverLogAdapter[IO](log)
      val users        = PostgresUserRepo.make(postgres)
      val ads          = PostgresAdRepo.make(postgres)
      val chats        = PostgresChatRepo.make(postgres)
      val msgs         = PostgresMessageRepo.make(postgres)
      fn(users, ads, chats, msgs)
    }

  private def msgGen(chatId: ChatId, sender: UserId) =
    for
      text <- msgTextGen
      at   <- instantGen
    yield Message(sender, chatId, text, at)

  private val gen =
    for
      u   <- userGen
      u1  <- userGen
      ad0 <- adGen
      ad = ad0.copy(authorId = u.id)
      chatId <- chatIdGen
      chat = Chat(chatId, adId = ad.id, u.id, u1.id)
      msg <- msgGen(chatId, u.id)
    yield (u, u1, ad, chat, msg)

  msgTest("send and get history") { (users, ads, chats, msgs) =>
    forall(gen) { (u, u1, ad, chat, msg) =>
      for
        _ <- users.create(u)
        _ <- users.create(u1)
        _ <- ads.create(ad)
        _ <- chats.create(chat)
        _ <- msgs.send(msg)
        x <- msgs.chatHistory(chat.id)
      yield expect.same(List(msg), x.map(_.copy(at = msg.at)))
    }
  }
