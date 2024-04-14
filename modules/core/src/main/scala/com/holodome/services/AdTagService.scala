package com.holodome.services

import cats.syntax.all._
import cats.Monad
import com.holodome.domain.ads.{AdId, AdTag}
import com.holodome.repositories.TagRepository

trait AdTagService[F[_]] {
  def all: F[List[AdTag]]
  def find(tag: AdTag): F[List[AdId]]
}

object AdTagService {
  def make[F[_]: Monad](tagRepo: TagRepository[F]): AdTagService[F] =
    new AdTagServiceInterpreter[F](tagRepo)

  private final class AdTagServiceInterpreter[F[_]: Monad](tagRepo: TagRepository[F])
      extends AdTagService[F] {

    override def all: F[List[AdTag]] =
      tagRepo.getAllTags

    override def find(tag: AdTag): F[List[AdId]] =
      tagRepo.getAllAdsByTag(tag).map(_.toList)
  }
}
