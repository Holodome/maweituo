package maweituo.tests.services

import maweituo.domain.ads.repos.{AdImageRepo, AdRepo, ChatRepo}
import maweituo.domain.services.IAMService
import maweituo.interp.IAMServiceInterp
import maweituo.tests.repos.*

import cats.effect.IO

def makeIAMService: IAMService[IO] =
  IAMServiceInterp.make[IO](new TestAdRepo, new TestChatRepo, new TestAdImageRepo)

def makeIAMService(ads: AdRepo[IO]): IAMService[IO] =
  IAMServiceInterp.make[IO](ads, new TestChatRepo, new TestAdImageRepo)

def makeIAMService(ads: AdRepo[IO], chats: ChatRepo[IO]): IAMService[IO] =
  IAMServiceInterp.make[IO](ads, chats, new TestAdImageRepo)

def makeIAMService(ads: AdRepo[IO], chats: ChatRepo[IO], images: AdImageRepo[IO]): IAMService[IO] =
  IAMServiceInterp.make[IO](ads, chats, images)

def makeIAMService(ads: AdRepo[IO], images: AdImageRepo[IO]): IAMService[IO] =
  IAMServiceInterp.make[IO](ads, new TestChatRepo, images)
