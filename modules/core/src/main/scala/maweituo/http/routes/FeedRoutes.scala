package maweituo.http.routes

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.syntax.all.*
import cats.{MonadThrow, Parallel}

import maweituo.domain.ads.{AdSearchRequest, AdSortOrder, AdTag}
import maweituo.domain.services.FeedService
import maweituo.domain.users.AuthedUser
import maweituo.domain.{Identity, Pagination}
import maweituo.http.BothRoutes
import maweituo.http.dto.FeedResponseDto
import maweituo.http.vars.UserIdVar

import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, HttpRoutes, QueryParamDecoder}

final case class FeedRoutes[F[_]: MonadThrow: JsonDecoder: Parallel](feed: FeedService[F])
    extends Http4sDsl[F] with BothRoutes[F]:

  private object PageMatcher     extends OptionalQueryParamDecoderMatcher[Int]("page")
  private object PageSizeMatcher extends OptionalQueryParamDecoderMatcher[Int]("pageSize")
  private object OrderMatcher    extends OptionalQueryParamDecoderMatcher[String]("order")
  private object TitleMatcher    extends OptionalQueryParamDecoderMatcher[String]("string")
  private object TagsMatcher     extends OptionalQueryParamDecoderMatcher[String]("tags")

  override val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "feed" :? PageMatcher(page) :? PageSizeMatcher(pageSize) :? OrderMatcher(order)
        :? TitleMatcher(title) :? TagsMatcher(filterTags) =>
      Validation.validateUnathorized(page, pageSize, order, filterTags, title) match
        case Valid(req) =>
          feed.feed(req)
            .map(FeedResponseDto.fromDomain)
            .flatMap(Ok(_))
        case Invalid(_) => BadRequest()
  }

  override val authRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case GET -> Root / "feed" / UserIdVar(userId) :? PageMatcher(page) :? PageSizeMatcher(pageSize)
        :? OrderMatcher(order) :? TitleMatcher(title) :? TagsMatcher(filterTags) as user =>
      if userId == user.id then
        given Identity = Identity(user.id)
        Validation.validateAuthorized(page, pageSize, order, filterTags, title) match
          case Valid(req) =>
            feed.feed(req)
              .map(FeedResponseDto.fromDomain)
              .flatMap(Ok(_))
          case Invalid(_) => BadRequest()
      else
        Forbidden()
  }

object Validation:

  enum SearchValidation:
    case InvalidPage
    case InvalidPageSize
    case UnauthorizedForRecs
    case InvalidSearch
    case EmptySearchTags

  type ValidationResult[A] = ValidatedNel[SearchValidation, A]

  private def validatePage(page: Option[Int]): ValidationResult[Int] =
    page match
      case Some(value) if value < 1 => SearchValidation.InvalidPage.invalidNel
      case Some(value)              => value.valid
      case None                     => 1.valid

  private def validatePageSize(pageSize: Option[Int]): ValidationResult[Int] =
    pageSize match
      case Some(value) if value < 1 => SearchValidation.InvalidPageSize.invalidNel
      case Some(value)              => value.valid
      case None                     => 10.valid

  private def validatePagination(page: Option[Int], pageSize: Option[Int]): ValidationResult[Pagination] =
    (validatePage(page), validatePageSize(pageSize)).mapN(Pagination.apply)

  private def validateFilterTagsStr(tags: String): ValidationResult[NonEmptyList[AdTag]] =
    NonEmptyList.fromList(tags.split(",").toList) match
      case Some(lst) => lst.map(AdTag.apply).valid
      case None      => SearchValidation.EmptySearchTags.invalidNel

  private def validateFilterTags(tags: Option[String]): ValidationResult[Option[NonEmptyList[AdTag]]] =
    tags match
      case Some(value) => validateFilterTagsStr(value).map(_.some)
      case None        => None.valid

  private def validateSortOrderUnauthorizedStr(order: String): ValidationResult[AdSortOrder] =
    order match
      case "created" => AdSortOrder.CreatedAtAsc.valid
      case "updated" => AdSortOrder.UpdatedAtAsc.valid
      case "title"   => AdSortOrder.Alphabetic.valid
      case "author"  => AdSortOrder.Author.valid
      case "recs"    => SearchValidation.UnauthorizedForRecs.invalidNel
      case _         => SearchValidation.InvalidSearch.invalidNel

  private def validateSortOrderUnauthorized(order: Option[String]): ValidationResult[AdSortOrder] =
    order match
      case Some(value) => validateSortOrderUnauthorizedStr(value)
      case None        => AdSortOrder.UpdatedAtAsc.valid

  private def validateSortOrderAuthorizedStr(order: String)(using id: Identity): ValidationResult[AdSortOrder] =
    order match
      case "created" => AdSortOrder.CreatedAtAsc.valid
      case "updated" => AdSortOrder.UpdatedAtAsc.valid
      case "title"   => AdSortOrder.Alphabetic.valid
      case "author"  => AdSortOrder.Author.valid
      case "recs"    => AdSortOrder.Recs(id).valid
      case _         => SearchValidation.InvalidSearch.invalidNel

  private def validateSortOrderAuthorized(order: Option[String])(using Identity): ValidationResult[AdSortOrder] =
    order match
      case Some(value) => validateSortOrderAuthorizedStr(value)
      case None        => AdSortOrder.UpdatedAtAsc.valid

  private def validateTitle(title: Option[String]): ValidationResult[Option[String]] =
    title.valid

  def validateUnathorized(
      page: Option[Int],
      pageSize: Option[Int],
      order: Option[String],
      tags: Option[String],
      title: Option[String]
  ): ValidationResult[AdSearchRequest] =
    (
      validatePagination(page, pageSize),
      validateSortOrderUnauthorized(order),
      validateFilterTags(tags),
      validateTitle(title)
    ).mapN(AdSearchRequest.apply)

  def validateAuthorized(
      page: Option[Int],
      pageSize: Option[Int],
      order: Option[String],
      tags: Option[String],
      title: Option[String]
  )(using Identity): ValidationResult[AdSearchRequest] =
    (
      validatePagination(page, pageSize),
      validateSortOrderAuthorized(order),
      validateFilterTags(tags),
      validateTitle(title)
    ).mapN(AdSearchRequest.apply)
