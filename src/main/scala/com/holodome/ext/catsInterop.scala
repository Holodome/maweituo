package com.holodome.ext

import com.outworkers.phantom.dsl.Database

import java.util.concurrent.CompletableFuture
import scala.concurrent.Future
import scala.jdk.FutureConverters.CompletionStageOps
import _root_.cats.effect.{Sync, Async, Resource}

object catsInterop {

  def makeDbResource[F[_]: Sync, D <: Database[D]](db: => D): Resource[F, D] =
    Resource.make(Sync[F].blocking(db))(db => Sync[F].blocking(db.shutdown()))

  def liftFuture[F[_]: Async, R](r: => Future[R]): F[R] =
    Async[F].fromFuture(Sync[F].delay(r))

  def liftJavaFuture[F[_]: Async, R](r: => CompletableFuture[R]): F[R] =
    liftFuture(r.asScala)
}
