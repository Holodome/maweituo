package maweituo
package domain
package services

import cats.data.Validated

import maweituo.domain.all.*
import maweituo.logic.DomainError
import maweituo.logic.search.{validateAuthorized, validateUnathorized}

trait FeedService[F[_]]:
  def feed(req: AdSearchRequest): F[PaginatedCollection[AdId]]

object FeedService:
  extension [F[_]: MonadThrow](service: FeedService[F])
    def feedUnauthorized(form: AdSearchForm): F[PaginatedCollection[AdId]] =
      validateUnathorized(form.page, form.pageSize, form.order, form.tags, form.title) match
        case Validated.Valid(req) => service.feed(req)
        case Validated.Invalid(e) => DomainError.InvalidSearchParams(e).raiseError[F, PaginatedCollection[AdId]]

    def feedAuthorized(form: AdSearchForm)(using Identity): F[PaginatedCollection[AdId]] =
      validateAuthorized(form.page, form.pageSize, form.order, form.tags, form.title) match
        case Validated.Valid(req) => service.feed(req)
        case Validated.Invalid(e) => DomainError.InvalidSearchParams(e).raiseError[F, PaginatedCollection[AdId]]
