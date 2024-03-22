package com.holodome.ext.phantom

import cats.effect.{Async, Resource, Sync}
import com.outworkers.phantom.dsl.Database

import scala.concurrent.Future

object catsInterop {

  // Database queries return Future[R]. We want to lift that into F. Luckily, cats-effect
  // provides Async.fromFuture, which is commonly used as IO.fromFuture(IO { makeR }).
  // This function does exactly that.
  // Be careful, however, as r() is assumed to be non-blocking.
  def liftFuture[F[_]: Async, R](r: => Future[R]): F[R] =
    Async[F].fromFuture(Sync[F].delay(r))

  // In phantom we extend database class D from Database[D]. Resulting class, when
  // constructed using cassandra config represents connection and interface to
  // cassandra table. This connection should also be closed. That is why we wrap it
  // into resource.
  def makeDbResource[F[_]: Sync, D <: Database[D]](db: => D): Resource[F, D] =
    Resource.make(Sync[F].blocking(db))(db => Sync[F].blocking(db.shutdown()))
}
