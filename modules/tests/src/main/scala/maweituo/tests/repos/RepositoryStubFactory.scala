package maweituo.tests.repos

import java.time.Instant

import scala.concurrent.duration.FiniteDuration

import cats.Monad
import cats.data.OptionT
import cats.syntax.all.*

import maweituo.domain.ads.images.{Image, ImageId}
import maweituo.domain.ads.messages.{Chat, ChatId, Message}
import maweituo.domain.ads.repos.{AdImageRepo, AdRepo, AdTagRepo, ChatRepo, MessageRepo}
import maweituo.domain.ads.{AdId, AdTag, Advertisement}
import maweituo.domain.pagination.Pagination
import maweituo.domain.repos.FeedRepo
import maweituo.domain.users.*
import maweituo.domain.users.repos.UserRepo

object RepoStubFactory:
  private inline def unit[F[_]: Monad]            = Monad[F].unit
  private inline def pure[F[_]: Monad, A](a: A)   = Monad[F].pure(a)
  private inline def emptyList[F[_]: Monad, A]    = List[A]().pure[F]
  private inline def emptyOptionT[F[_]: Monad, A] = OptionT(none[A].pure[F])

  def images[F[_]: Monad]: AdImageRepo[F] = new:
    def create(image: Image)     = unit
    def find(imageId: ImageId)   = emptyOptionT
    def findIdsByAd(adId: AdId)  = emptyList
    def delete(imageId: ImageId) = unit

  def ads[F[_]: Monad]: AdRepo[F] = new:
    def create(ad: Advertisement): F[Unit]             = unit
    def all: F[List[Advertisement]]                    = emptyList
    def find(id: AdId): OptionT[F, Advertisement]      = emptyOptionT
    def findIdsByAuthor(userId: UserId): F[List[AdId]] = emptyList
    def markAsResolved(id: AdId): F[Unit]              = unit
    def delete(id: AdId): F[Unit]                      = unit

  def chats[F[_]: Monad]: ChatRepo[F] = new:
    def create(chat: Chat): F[Unit]                                     = unit
    def find(chatId: ChatId): OptionT[F, Chat]                          = emptyOptionT
    def findByAdAndClient(adId: AdId, client: UserId): OptionT[F, Chat] = emptyOptionT

  def msgs[F[_]: Monad]: MessageRepo[F] = new:
    def chatHistory(chatId: ChatId): F[List[Message]] = emptyList
    def send(message: Message): F[Unit]               = unit

  def tags[F[_]: Monad]: AdTagRepo[F] = new:
    def getAdTags(adId: AdId): F[List[AdTag]]            = emptyList
    def getAllTags: F[List[AdTag]]                       = emptyList
    def addTagToAd(adId: AdId, tag: AdTag): F[Unit]      = unit
    def removeTagFromAd(adId: AdId, tag: AdTag): F[Unit] = unit
    def getAllAdsByTag(tag: AdTag): F[List[AdId]]        = emptyList

  def users[F[_]: Monad]: UserRepo[F] = new:
    def create(request: User): F[Unit]               = unit
    def all: F[List[User]]                           = emptyList
    def find(userId: UserId): OptionT[F, User]       = emptyOptionT
    def findByEmail(email: Email): OptionT[F, User]  = emptyOptionT
    def findByName(name: Username): OptionT[F, User] = emptyOptionT
    def delete(id: UserId): F[Unit]                  = unit
    def update(update: UpdateUserRepoRequest): F[Unit]  = unit

  def feed[F[_]: Monad]: FeedRepo[F] = new:
    def getPersonalizedSize(user: UserId): F[Int]                                          = pure(0)
    def getPersonalized(user: UserId, pag: Pagination): F[List[AdId]]                      = emptyList
    def getGlobalSize: F[Int]                                                              = pure(0)
    def getGlobal(pag: Pagination): F[List[AdId]]                                          = emptyList
    def setPersonalized(userId: UserId, ads: List[AdId], ttlSecs: FiniteDuration): F[Unit] = unit
    def addToGlobalFeed(ad: AdId, at: Instant): F[Unit]                                    = unit
