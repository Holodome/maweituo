package maweituo
package tests
package repos

private inline def makeError(name: String) =
  IO.raiseError(new Exception("Unexpected call to " + name))

class TestUserRepo extends UserRepo[IO]:
  override def findByName(name: Username): OptionT[IO, User]   = OptionT(makeError("TestUserRepo.findByName"))
  override def delete(id: UserId): IO[Unit]                    = makeError("TestUserRepo.delete")
  override def all: IO[List[User]]                             = makeError("TestUserRepo.all")
  override def findByEmail(email: Email): OptionT[IO, User]    = OptionT(makeError("TestUserRepo.findByEmail"))
  override def create(request: User): IO[Unit]                 = makeError("TestUserRepo.create")
  override def find(userId: UserId): OptionT[IO, User]         = OptionT(makeError("TestUserRepo.find"))
  override def update(update: UpdateUserRepoRequest): IO[Unit] = makeError("TestUserRepo.update")

class TestAdRepo extends AdRepo[IO]:
  override def delete(id: AdId): IO[Unit]                      = makeError("TestAdRepo.delete")
  override def findIdsByAuthor(userId: UserId): IO[List[AdId]] = makeError("TestAdRepo.findIdsByAuthor")
  override def create(ad: Advertisement): IO[Unit]             = makeError("TestAdRepo.create")
  override def find(id: AdId): OptionT[IO, Advertisement]      = OptionT(makeError("TestAdRepo.find"))
  def update(req: UpdateAdRepoRequest): IO[Unit]               = makeError("TestAdRepo.update")

class TestChatRepo extends ChatRepo[IO]:
  override def create(chat: Chat): IO[Unit]            = makeError("TestChatRepo.create")
  override def find(chatId: ChatId): OptionT[IO, Chat] = OptionT(makeError("TestChatRepo.find"))
  override def findByAdAndClient(adId: AdId, client: UserId): OptionT[IO, Chat] =
    OptionT(makeError("TestChatRepo.findByAdAndClient"))
  override def findForAd(ad: AdId): IO[List[Chat]] = makeError("TestChatRepo.findForAd")

class TestMessageRepo extends MessageRepo[IO]:
  override def chatHistory(chatId: ChatId): IO[List[Message]] = makeError("TestMessageRepo.chatHistory")
  override def send(message: Message): IO[Unit]               = makeError("TestMessageRepo.send")

class TestAdImageRepo extends AdImageRepo[IO]:
  override def create(image: Image): IO[Unit]             = makeError("TestAdImageRepo.create")
  override def find(imageId: ImageId): OptionT[IO, Image] = OptionT(makeError("TestAdImageRepo.find"))
  override def findIdsByAd(adId: AdId): IO[List[ImageId]] = makeError("TestAdImageRepo.findIdsByAd")
  override def delete(imageId: ImageId): IO[Unit]         = makeError("TestAdImageRepo.delete")
