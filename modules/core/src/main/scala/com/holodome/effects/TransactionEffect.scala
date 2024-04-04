package com.holodome.effects

import cats.syntax.all._
import cats.{FlatMap, MonadError}

class TransactionEffect[F[_], E](
    underlying: F[E],
    rollback: PartialFunction[Throwable, F[Unit]]
)(implicit
    F: FlatMap[F],
    ME: MonadError[F, Throwable]
) {
  def flatMap[S](f: E => F[S]): F[S] = {
    underlying.flatMap(f).recoverWith { case e: Throwable =>
      val failure: F[S] = ME.raiseError[S](e)
      rollback.lift(e).fold(failure)(_.flatMap(_ => failure))
    }
  }
}

object TransactionEffect {
  implicit class TransactionEffectSyntax[F[_], E](underlying: F[E])(implicit
      F: FlatMap[F],
      ME: MonadError[F, Throwable]
  ) {
    def rollbackWith(rollback: PartialFunction[Throwable, F[Unit]]): TransactionEffect[F, E] =
      new TransactionEffect[F, E](underlying, rollback)
  }
}
