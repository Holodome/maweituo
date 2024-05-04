package com.holodome.services.interpreters

import cats.Monad
import cats.syntax.all._
import com.holodome.domain.ads.AdId
import com.holodome.domain.ads.AdTag
import com.holodome.repositories.TagRepository
import com.holodome.services.AdTagService

object AdTagServiceInterpreter {

  def make[F[_]: Monad](tagRepo: TagRepository[F]): AdTagService[F] =
    new AdTagServiceInterpreter[F](tagRepo)

}

private final class AdTagServiceInterpreter[F[_]: Monad](tagRepo: TagRepository[F])
    extends AdTagService[F] {

  override def all: F[List[AdTag]] =
    tagRepo.getAllTags

  override def find(tag: AdTag): F[List[AdId]] =
    tagRepo.getAllAdsByTag(tag).map(_.toList)
}
