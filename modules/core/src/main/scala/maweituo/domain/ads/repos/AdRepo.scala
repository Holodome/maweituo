package maweituo.domain.ads.repos

import java.time.Instant

import cats.MonadThrow
import cats.data.OptionT

import maweituo.domain.ads.*
import maweituo.domain.users.UserId
import maweituo.logic.errors.DomainError

trait AdRepo[F[_]]:
  def create(ad: Advertisement): F[Unit]
  def find(id: AdId): OptionT[F, Advertisement]
  def findIdsByAuthor(userId: UserId): F[List[AdId]]
  def markAsResolved(id: AdId, at: Instant): F[Unit]
  def delete(id: AdId): F[Unit]

object AdRepo:
  extension [F[_]: MonadThrow](repo: AdRepo[F])
    def get(id: AdId): F[Advertisement] =
      repo.find(id).getOrRaise(DomainError.InvalidAdId(id))
