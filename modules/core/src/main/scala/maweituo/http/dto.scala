package maweituo.http.dto

import java.time.Instant

import cats.data.{EitherT, OptionT}
import cats.derived.derived
import cats.effect.Concurrent
import cats.syntax.all.*
import cats.{MonadThrow, Show}

import maweituo.domain.ads.images.{ImageContentsStream, MediaType}
import maweituo.domain.ads.messages.*
import maweituo.domain.ads.{AdId, AdTag, AdTitle, Advertisement, CreateAdRequest}
import maweituo.domain.Pagination
import maweituo.domain.users.*
import maweituo.utils.given

import dev.profunktor.auth.jwt.JwtToken
import io.circe.{Codec, Encoder}
import org.http4s.{EntityDecoder, MalformedMessageBodyFailure, Media, MediaRange}
import maweituo.domain.ads.PaginatedAdsResponse
import maweituo.domain.PaginatedCollection

final case class FeedResponseDto(
    items: List[AdId],
    pag: Pagination,
    totalPages: Int,
    totalItems: Int
) derives Codec.AsObject

object FeedResponseDto:
  def fromDomain(domain: PaginatedCollection[AdId]): FeedResponseDto =
    FeedResponseDto(domain.items, domain.pag, domain.totalPages, domain.totalItems)

  def fromDomain(domain: PaginatedAdsResponse): FeedResponseDto =
    FeedResponseDto(domain.col.items, domain.col.pag, domain.col.totalPages, domain.col.totalItems)

final case class LoginRequestDto(name: Username, password: Password) derives Codec.AsObject:
  def toDomain: LoginRequest = LoginRequest(name, password)

final case class LoginResponseDto(jwt: JwtToken) derives Codec.AsObject

final case class RegisterRequestDto(
    name: Username,
    email: Email,
    password: Password
) derives Codec.AsObject:
  def toDomain: RegisterRequest = RegisterRequest(name, email, password)

final case class RegisterResponseDto(user: UserId) derives Codec.AsObject

final case class UpdateUserRequestDto(
    id: UserId,
    name: Option[Username],
    email: Option[Email],
    password: Option[Password]
) derives Codec.AsObject:
  def toDomain: UpdateUserRequest = UpdateUserRequest(id, name, email, password)

final case class UserPublicInfoDto(
    id: UserId,
    name: Username,
    email: Email
) derives Codec.AsObject, Show

object UserPublicInfoDto:
  def fromUser(user: User): UserPublicInfoDto =
    UserPublicInfoDto(user.id, user.name, user.email)

final case class UserAdsResponseDto(ads: List[AdId]) derives Codec.AsObject

final case class AdTagsResponseDto(tags: List[AdTag]) derives Codec.AsObject

final case class AddTagRequestDto(
    tag: AdTag
) derives Codec.AsObject

final case class DeleteTagRequestDto(
    tag: AdTag
) derives Codec.AsObject

final case class AdDto(
    id: AdId,
    authorId: UserId,
    title: AdTitle,
    resolved: Boolean
) derives Codec.AsObject

object AdResponseDto:
  def fromDomain(ad: Advertisement): AdDto =
    AdDto(ad.id, ad.authorId, ad.title, ad.resolved)

final case class CreateAdRequestDto(
    title: AdTitle
) derives Codec.AsObject:
  def toDomain: CreateAdRequest = CreateAdRequest(title)

final case class CreateAdResponseDto(
    id: AdId
) derives Codec.AsObject

final case class MarkAdResolvedRequestDto(withWhom: UserId) derives Codec.AsObject

final case class MessageDto(
    sender: UserId,
    chat: ChatId,
    text: MessageText,
    at: Instant
) derives Codec.AsObject, Show

object MessageDto:
  def fromDomain(domain: Message): MessageDto =
    MessageDto(domain.sender, domain.chat, domain.text, domain.at)

final case class HistoryResponseDto(messages: List[MessageDto]) derives Codec.AsObject

object HistoryResponseDto:
  def fromDomain(resp: HistoryResponse): HistoryResponseDto =
    HistoryResponseDto(resp.messages.map(MessageDto.fromDomain))

final case class SendMessageRequestDto(text: MessageText) derives Codec.AsObject:
  def toDomain: SendMessageRequest = SendMessageRequest(text)

final case class ChatDto(
    id: ChatId,
    adId: AdId,
    adAuthor: UserId,
    client: UserId
) derives Codec.AsObject

object ChatDto:
  def fromDomain(domain: Chat): ChatDto =
    ChatDto(domain.id, domain.adId, domain.adAuthor, domain.client)

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
