package com.holodome.ext

import java.util.concurrent.CompletableFuture
import scala.concurrent.Future
import scala.jdk.FutureConverters.CompletionStageOps
import _root_.cats.effect.{Sync, Async}

object cats {

  def liftFuture[F[_]: Async, R](r: => Future[R]): F[R] =
    Async[F].fromFuture(Sync[F].delay(r))

  def liftCompletableFuture[F[_]: Async, R](r: => CompletableFuture[R]): F[R] =
    Async[F].fromCompletableFuture(Sync[F].delay(r))
}
