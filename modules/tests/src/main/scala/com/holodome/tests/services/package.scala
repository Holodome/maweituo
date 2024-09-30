package com.holodome.tests.services

import com.holodome.domain.services.IAMService
import cats.effect.IO
import com.holodome.tests.repositories.TestChatRepository
import com.holodome.tests.repositories.*
import com.holodome.interpreters.IAMServiceInterpreter
import com.holodome.domain.repositories.AdRepository

def makeIAMService: IAMService[IO] =
  IAMServiceInterpreter.make[IO](new TestAdRepository, new TestChatRepository, new TestAdImageRepository)

def makeIAMService(ads: AdRepository[IO]): IAMService[IO] =
  IAMServiceInterpreter.make[IO](ads, new TestChatRepository, new TestAdImageRepository)
