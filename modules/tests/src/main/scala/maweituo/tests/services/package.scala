package maweituo
package tests
package services

import maweituo.domain.all.*
import maweituo.logic.interp.all.*
import maweituo.tests.repos.*

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
