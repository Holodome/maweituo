package maweituo
package domain

import java.time.Instant
import java.util.UUID

import scala.util.Try

import cats.data.NonEmptyList
import cats.derived.derived
import cats.syntax.all.*
import cats.{Functor, Show}

import maweituo.infrastructure.effects.TimeSource
import maweituo.utils.{IdNewtype, Newtype, given}

object ads:
  import maweituo.domain.users.*

  type AdId = AdId.Type
  object AdId extends IdNewtype

  type AdTitle = AdTitle.Type
  object AdTitle extends Newtype[String]

  type AdTag = AdTag.Type
  object AdTag extends Newtype[String]

  final case class Advertisement(
      id: AdId,
      authorId: UserId,
      title: AdTitle,
      resolved: Boolean,
      createdAt: Instant,
      updatedAt: Instant
  ) derives Show

  final case class CreateAdRequest(
      title: AdTitle
  ) derives Show

  final case class AdParam(value: String) derives Show:
    def toDomain: Option[AdId] =
      Try(UUID.fromString(value)).map(AdId.apply).toOption

  enum AdSortOrder derives Show:
    case CreatedAtAsc, UpdatedAtAsc, Alphabetic, Author
    case Recs(user: UserId)

  object AdSortOrder:
    def default: AdSortOrder = AdSortOrder.UpdatedAtAsc
    def allBasic: List[AdSortOrder] = List(
      AdSortOrder.CreatedAtAsc,
      AdSortOrder.UpdatedAtAsc,
      AdSortOrder.Alphabetic,
      AdSortOrder.Author
    )

  final case class AdSearchRequest(
      pag: Pagination,
      order: AdSortOrder = AdSortOrder.default,
      filterTags: Option[NonEmptyList[AdTag]] = None,
      nameLike: Option[String] = None
  )

  final case class PaginatedAdsResponse(
      col: PaginatedCollection[AdId],
      sortOrder: AdSortOrder
  )

  object PaginatedAdsResponse:
    def empty: PaginatedAdsResponse =
      PaginatedAdsResponse(PaginatedCollection.empty, AdSortOrder.CreatedAtAsc)

  final case class AdSearchForm(
      page: Int,
      pageSize: Option[Int],
      order: Option[String],
      tags: Option[String],
      title: Option[String]
  ) derives Show

  final case class UpdateAdRequest(
      id: AdId,
      resolved: Option[Boolean],
      title: Option[AdTitle]
  )

  final case class UpdateAdRepoRequest(
      id: AdId,
      resolved: Option[Boolean],
      title: Option[AdTitle],
      at: Instant
  )

  object UpdateAdRepoRequest:
    def fromReq[F[_]: TimeSource: Functor](req: UpdateAdRequest): F[UpdateAdRepoRequest] =
      TimeSource[F].instant.map { at =>
        UpdateAdRepoRequest(
          req.id,
          req.resolved,
          req.title,
          at
        )
      }
