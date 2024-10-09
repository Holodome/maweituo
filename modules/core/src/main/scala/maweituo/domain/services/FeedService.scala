package maweituo.domain.services

import cats.MonadThrow
import cats.data.Validated
import cats.syntax.all.*

import maweituo.domain.ads.{AdId, AdSearchRequest}
import maweituo.domain.{Identity, PaginatedCollection}
import maweituo.logic.errors.DomainError
import maweituo.logic.search.{validateAuthorized, validateUnathorized}

final case class AdSearchForm(
    page: Option[Int],
    pageSize: Option[Int],
    order: Option[String],
    tags: Option[String],
    title: Option[String]
)

trait FeedService[F[_]]:
  def feed(req: AdSearchRequest): F[PaginatedCollection[AdId]]

extension [F[_]: MonadThrow](service: FeedService[F])
  def feedUnauthorized(form: AdSearchForm): F[PaginatedCollection[AdId]] =
    validateUnathorized(form.page, form.pageSize, form.order, form.tags, form.title) match
      case Validated.Valid(req) => service.feed(req)
      case Validated.Invalid(e) => DomainError.InvalidSearchParams(e).raiseError[F, PaginatedCollection[AdId]]

  def feedAuthorized(form: AdSearchForm)(using Identity): F[PaginatedCollection[AdId]] =
    validateAuthorized(form.page, form.pageSize, form.order, form.tags, form.title) match
      case Validated.Valid(req) => service.feed(req)
      case Validated.Invalid(e) => DomainError.InvalidSearchParams(e).raiseError[F, PaginatedCollection[AdId]]
