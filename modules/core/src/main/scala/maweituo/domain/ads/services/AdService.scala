package maweituo.domain.ads.services

import maweituo.domain.Identity
import maweituo.domain.ads.*
import maweituo.domain.users.UserId

trait AdService[F[_]]:
  def get(id: AdId): F[Advertisement]
  def create(create: CreateAdRequest)(using Identity): F[AdId]
  def delete(id: AdId)(using Identity): F[Unit]
  def markAsResolved(id: AdId, withWhom: UserId)(using Identity): F[Unit]
