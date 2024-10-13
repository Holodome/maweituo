package maweituo
package http

import java.time.Instant

import cats.data.{EitherT, OptionT}
import cats.derived.derived
import cats.effect.Concurrent
import cats.syntax.all.*
import cats.{MonadThrow, Show}

import maweituo.domain.all.*
import maweituo.utils.given

import dev.profunktor.auth.jwt.JwtToken
import io.circe.{Codec, Decoder, Encoder}
import org.http4s.{EntityDecoder, MalformedMessageBodyFailure, Media, MediaRange}

object dto:

  final case class ErrorResponseDto(errors: List[String]) derives Codec.AsObject

  object ErrorResponseDto:
    def make(e: String): ErrorResponseDto = ErrorResponseDto(List(e))

  given Encoder[Pagination] = Encoder.derived
  given Decoder[Pagination] = Decoder.derived

  given [A: Encoder]: Encoder[PaginatedCollection[A]] = Encoder.derived
  given [A: Decoder]: Decoder[PaginatedCollection[A]] = Decoder.derived

  final case class FeedResponseDto(
      feed: PaginatedCollection[AdId]
  ) derives Codec.AsObject

  final case class LoginRequestDto(name: Username, password: Password) derives Codec.AsObject:
    def toDomain: LoginRequest = LoginRequest(name, password)

  final case class LoginResponseDto(jwt: JwtToken) derives Codec.AsObject

  final case class RegisterRequestDto(
      name: Username,
      email: Email,
      password: Password
  ) derives Codec.AsObject:
    def toDomain: RegisterRequest = RegisterRequest(name, email, password)

  final case class RegisterResponseDto(userId: UserId) derives Codec.AsObject

  final case class UpdateUserRequestDto(
      name: Option[Username],
      email: Option[Email],
      password: Option[Password]
  ) derives Codec.AsObject:
    def toDomain(userId: UserId): UpdateUserRequest = UpdateUserRequest(userId, name, email, password)

  final case class UserPublicInfoDto(
      id: UserId,
      name: Username,
      email: Email
  ) derives Codec.AsObject, Show

  object UserPublicInfoDto:
    def fromUser(user: User): UserPublicInfoDto =
      UserPublicInfoDto(user.id, user.name, user.email)

  final case class UserAdsResponseDto(userId: UserId, ads: List[AdId]) derives Codec.AsObject
  final case class UserChatsResponseDto(userId: UserId, chats: List[ChatDto]) derives Codec.AsObject

  object UserChatsResponseDto:
    def fromDomain(userId: UserId, chats: List[Chat]): UserChatsResponseDto =
      UserChatsResponseDto(userId, chats.map(ChatDto.fromDomain))

  final case class AdTagsResponseDto(adId: AdId, tags: List[AdTag]) derives Codec.AsObject

  final case class AddTagRequestDto(
      tag: AdTag
  ) derives Codec.AsObject

  final case class DeleteTagRequestDto(
      tag: AdTag
  ) derives Codec.AsObject

  final case class AdResponseDto(
      id: AdId,
      authorId: UserId,
      title: AdTitle,
      resolved: Boolean,
      createdAt: Instant,
      updatedAt: Instant
  ) derives Codec.AsObject

  object AdResponseDto:
    def fromDomain(ad: Advertisement): AdResponseDto =
      AdResponseDto(ad.id, ad.authorId, ad.title, ad.resolved, ad.createdAt, ad.updatedAt)

  final case class CreateAdRequestDto(
      title: AdTitle
  ) derives Codec.AsObject:
    def toDomain: CreateAdRequest = CreateAdRequest(title)

  final case class CreateAdResponseDto(
      id: AdId
  ) derives Codec.AsObject

  final case class UpdateAdRequestDto(
      resolved: Option[Boolean],
      title: Option[AdTitle]
  ) derives Codec.AsObject:
    def toDomain(adId: AdId): UpdateAdRequest = UpdateAdRequest(adId, resolved, title)

  final case class MarkAdResolvedRequestDto(withWhom: UserId) derives Codec.AsObject

  final case class MessageDto(
      senderId: UserId,
      chatId: ChatId,
      text: MessageText,
      at: java.time.Instant
  ) derives Codec.AsObject

  object MessageDto:
    def fromDomain(domain: Message): MessageDto =
      MessageDto(domain.sender, domain.chat, domain.text, domain.at)

  final case class HistoryResponseDto(
      chatId: ChatId,
      messages: PaginatedCollection[MessageDto]
  ) derives Codec.AsObject

  object HistoryResponseDto:
    def fromDomain(chatId: ChatId, messages: PaginatedCollection[Message]): HistoryResponseDto =
      HistoryResponseDto(chatId, messages.map(MessageDto.fromDomain))

  final case class SendMessageRequestDto(text: MessageText) derives Codec.AsObject:
    def toDomain: SendMessageRequest = SendMessageRequest(text)

  final case class ChatDto(
      id: ChatId,
      adId: AdId,
      adAuthor: UserId,
      clientId: UserId
  ) derives Codec.AsObject

  object ChatDto:
    def fromDomain(domain: Chat): ChatDto =
      ChatDto(domain.id, domain.adId, domain.adAuthor, domain.client)

  final case class AdChatsResponseDto(
      adId: AdId,
      chats: List[ChatDto]
  ) derives Codec.AsObject

  final case class UploadImageRequestDto[+F[_]](
      data: fs2.Stream[F, Byte],
      contentType: MediaType,
      dataSize: Long
  ):
    def toDomain: ImageContentsStream[F] = ImageContentsStream[F](data, contentType, dataSize)

  object UploadImageRequestDto:
    given [F[_]: MonadThrow: Concurrent]: EntityDecoder[F, UploadImageRequestDto[F]] =
      EntityDecoder.decodeBy(MediaRange.`image/*`) { (m: Media[F]) =>
        EitherT.liftF(
          (
            OptionT
              .fromOption(m.contentType)
              .getOrRaise(MalformedMessageBodyFailure("Expected Content-Type header")),
            OptionT
              .fromOption(m.contentLength)
              .getOrRaise(MalformedMessageBodyFailure("Expected Content-Length header"))
          ).tupled
            .map { case (contentType, contentLength) =>
              UploadImageRequestDto(
                m.body,
                MediaType(contentType.mediaType.mainType, contentType.mediaType.subType),
                contentLength
              )
            }
        )
      }

  final case class AllTagsResponse(tags: List[AdTag]) derives Codec.AsObject

  final case class TagAdsResponse(tag: AdTag, adIds: List[AdId]) derives Codec.AsObject
