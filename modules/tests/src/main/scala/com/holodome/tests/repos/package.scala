package com.holodome.tests.repos

import com.holodome.domain.ads.repos.*
import com.holodome.domain.ads.{ AdId, Advertisement }
import com.holodome.domain.images.*
import com.holodome.domain.messages.*
import com.holodome.domain.users.repos.UserRepository
import com.holodome.domain.users.{ Email, UpdateUserInternal, User, UserId, Username }

import cats.data.OptionT
import cats.effect.IO

private inline def makeError(name: String) =
  IO.raiseError(new Exception("Unexpected call to " + name))

class TestUserRepository extends UserRepository[IO]:
  override def findByName(name: Username): OptionT[IO, User] = OptionT(makeError("TestUserRepository.findByName"))
  override def delete(id: UserId): IO[Unit]                  = makeError("TestUserRepository.delete")
  override def all: IO[List[User]]                           = makeError("TestUserRepository.all")
  override def findByEmail(email: Email): OptionT[IO, User]  = OptionT(makeError("TestUserRepository.findByEmail"))
  override def create(request: User): IO[Unit]               = makeError("TestUserRepository.create")
  override def find(userId: UserId): OptionT[IO, User]       = OptionT(makeError("TestUserRepository.find"))
  override def update(update: UpdateUserInternal): IO[Unit]  = makeError("TestUserRepository.update")

class TestAdRepository extends AdRepository[IO]:
  override def delete(id: AdId): IO[Unit]                      = makeError("TestAdRepository.delete")
  override def findIdsByAuthor(userId: UserId): IO[List[AdId]] = makeError("TestAdRepository.findIdsByAuthor")
  override def create(ad: Advertisement): IO[Unit]             = makeError("TestAdRepository.create")
  override def find(id: AdId): OptionT[IO, Advertisement]      = OptionT(makeError("TestAdRepository.find"))
  override def all: IO[List[Advertisement]]                    = makeError("TestAdRepository.all")
  override def markAsResolved(id: AdId): IO[Unit]              = makeError("TestAdRepository.markAsResolved")

class TestChatRepository extends ChatRepository[IO]:
  override def create(chat: Chat): IO[Unit]            = makeError("TestChatRepository.create")
  override def find(chatId: ChatId): OptionT[IO, Chat] = OptionT(makeError("TestChatRepository.find"))
  override def findByAdAndClient(adId: AdId, client: UserId): OptionT[IO, Chat] =
    OptionT(makeError("TestChatRepository.findByAdAndClient"))

class TestMessageRepository extends MessageRepository[IO]:
  override def chatHistory(chatId: ChatId): IO[List[Message]] = makeError("TestMessageRepository.chatHistory")
  override def send(message: Message): IO[Unit]               = makeError("TestMessageRepository.send")

class TestAdImageRepository extends AdImageRepository[IO]:
  override def create(image: Image): IO[Unit]             = makeError("TestAdImageRepository.create")
  override def find(imageId: ImageId): OptionT[IO, Image] = OptionT(makeError("TestAdImageRepository.find"))
  override def findIdsByAd(adId: AdId): IO[List[ImageId]] = makeError("TestAdImageRepository.findIdsByAd")
  override def delete(imageId: ImageId): IO[Unit]         = makeError("TestAdImageRepository.delete")
