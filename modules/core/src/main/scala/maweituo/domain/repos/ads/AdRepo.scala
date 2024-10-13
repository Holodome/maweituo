package maweituo
package domain
package repos
package ads

import cats.MonadThrow
import cats.data.OptionT

import maweituo.domain.ads.*
import maweituo.domain.users.UserId

trait AdRepo[F[_]]:
  def create(ad: Advertisement): F[Unit]
  def find(id: AdId): OptionT[F, Advertisement]
  def findIdsByAuthor(userId: UserId): F[List[AdId]]
  def update(req: UpdateAdRepoRequest): F[Unit]
  def delete(id: AdId): F[Unit]

object AdRepo:
  import maweituo.logic.errors.DomainError

  extension [F[_]: MonadThrow](repo: AdRepo[F])
    def get(id: AdId): F[Advertisement] =
      repo.find(id).getOrRaise(DomainError.InvalidAdId(id))
