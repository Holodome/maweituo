package maweituo.domain.services

import cats.syntax.all.*
import maweituo.domain.PaginatedCollection
import maweituo.domain.ads.{AdId, AdSearchRequest}
import cats.MonadThrow
import maweituo.logic.search.{validateAuthorized, validateUnathorized}
import cats.data.Validated
import maweituo.domain.errors.DomainError
import maweituo.domain.Identity

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
