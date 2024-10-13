package maweituo
package domain
package services
package ads

import maweituo.domain.ads.*

trait AdService[F[_]]:
  def get(id: AdId): F[Advertisement]
  def create(create: CreateAdRequest)(using Identity): F[AdId]
  def delete(id: AdId)(using Identity): F[Unit]
  def update(req: UpdateAdRequest)(using Identity): F[Unit]
