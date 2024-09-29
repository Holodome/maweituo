package com.holodome.interpreters

import com.holodome.domain.ads.AdId
import com.holodome.domain.ads.AdTag
import com.holodome.domain.repositories.TagRepository
import com.holodome.domain.services.AdTagService

import cats.Monad
import cats.syntax.all.*

object AdTagServiceInterpreter:
  def make[F[_]: Monad](tagRepo: TagRepository[F]): AdTagService[F] = new:
    def all: F[List[AdTag]] =
      tagRepo.getAllTags

    def find(tag: AdTag): F[List[AdId]] =
      tagRepo.getAllAdsByTag(tag).map(_.toList)
