package maweituo
package logic
package search

import scala.util.control.NoStackTrace

import cats.data.{Validated, ValidatedNel}

import maweituo.domain.all.*

enum SearchValidationError extends NoStackTrace derives Show:
  case InvalidPage
  case NoPage
  case InvalidPageSize
  case UnauthorizedForRecs
  case InvalidSearch
  case EmptySearchTags

type ValidationResult[A] = ValidatedNel[SearchValidationError, A]

private def validatePage(page: Option[Int]): ValidationResult[Int] =
  page match
    case Some(value) if value < 1 => SearchValidationError.InvalidPage.invalidNel
    case Some(value)              => value.valid
    case None                     => SearchValidationError.NoPage.invalidNel

private def validatePageSize(pageSize: Option[Int]): ValidationResult[Int] =
  pageSize match
    case Some(value) if value < 1 => SearchValidationError.InvalidPageSize.invalidNel
    case Some(value)              => value.valid
    case None                     => Pagination.defaultPageSize.valid

private def validatePagination(page: Option[Int], pageSize: Option[Int]): ValidationResult[Pagination] =
  (validatePage(page), validatePageSize(pageSize)).mapN(Pagination.apply)

private def validateFilterTagsStr(tags: String): ValidationResult[NonEmptyList[AdTag]] =
  NonEmptyList.fromList(tags.split(",").toList) match
    case Some(lst) => lst.map(AdTag.apply).valid
    case None      => SearchValidationError.EmptySearchTags.invalidNel

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
    case "recs"    => SearchValidationError.UnauthorizedForRecs.invalidNel
    case _         => SearchValidationError.InvalidSearch.invalidNel

private def validateSortOrderUnauthorized(order: Option[String]): ValidationResult[AdSortOrder] =
  order match
    case Some(value) => validateSortOrderUnauthorizedStr(value)
    case None        => AdSortOrder.default.valid

private def validateSortOrderAuthorizedStr(order: String)(using id: Identity): ValidationResult[AdSortOrder] =
  order match
    case "created" => AdSortOrder.CreatedAtAsc.valid
    case "updated" => AdSortOrder.UpdatedAtAsc.valid
    case "title"   => AdSortOrder.Alphabetic.valid
    case "author"  => AdSortOrder.Author.valid
    case "recs"    => AdSortOrder.Recs(id).valid
    case _         => SearchValidationError.InvalidSearch.invalidNel

private def validateSortOrderAuthorized(order: Option[String])(using Identity): ValidationResult[AdSortOrder] =
  order match
    case Some(value) => validateSortOrderAuthorizedStr(value)
    case None        => AdSortOrder.default.valid

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
