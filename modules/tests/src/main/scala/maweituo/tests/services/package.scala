package maweituo.tests.services

import maweituo.domain.ads.repos.{AdImageRepository, AdRepository, ChatRepository}
import maweituo.domain.services.IAMService
import maweituo.interpreters.IAMServiceInterpreter
import maweituo.tests.repos.*

import cats.effect.IO

def makeIAMService: IAMService[IO] =
  IAMServiceInterpreter.make[IO](new TestAdRepository, new TestChatRepository, new TestAdImageRepository)

def makeIAMService(ads: AdRepository[IO]): IAMService[IO] =
  IAMServiceInterpreter.make[IO](ads, new TestChatRepository, new TestAdImageRepository)

def makeIAMService(ads: AdRepository[IO], chats: ChatRepository[IO]): IAMService[IO] =
  IAMServiceInterpreter.make[IO](ads, chats, new TestAdImageRepository)

def makeIAMService(ads: AdRepository[IO], chats: ChatRepository[IO], images: AdImageRepository[IO]): IAMService[IO] =
  IAMServiceInterpreter.make[IO](ads, chats, images)

def makeIAMService(ads: AdRepository[IO], images: AdImageRepository[IO]): IAMService[IO] =
  IAMServiceInterpreter.make[IO](ads, new TestChatRepository, images)
