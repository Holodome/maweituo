package com.holodome.tests.services

import com.holodome.domain.ads.repos.{ AdRepository, ChatRepository }
import com.holodome.domain.services.IAMService
import com.holodome.interpreters.IAMServiceInterpreter
import com.holodome.tests.repos.*

import cats.effect.IO

def makeIAMService: IAMService[IO] =
  IAMServiceInterpreter.make[IO](new TestAdRepository, new TestChatRepository, new TestAdImageRepository)

def makeIAMService(ads: AdRepository[IO]): IAMService[IO] =
  IAMServiceInterpreter.make[IO](ads, new TestChatRepository, new TestAdImageRepository)

def makeIAMService(ads: AdRepository[IO], chats: ChatRepository[IO]): IAMService[IO] =
  IAMServiceInterpreter.make[IO](ads, chats, new TestAdImageRepository)
