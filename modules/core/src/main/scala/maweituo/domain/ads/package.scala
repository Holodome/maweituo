package maweituo.domain.ads

import java.time.Instant
import java.util.UUID

import scala.util.Try

import cats.Show
import cats.data.NonEmptyList
import cats.derived.*
import cats.kernel.Eq
import cats.syntax.all.*

import maweituo.domain.users.UserId
import maweituo.domain.{PaginatedCollection, Pagination}
import maweituo.utils.{IdNewtype, Newtype, given}

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

final case class AdParam(value: String) derives Eq, Show:
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
