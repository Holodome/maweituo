package maweituo.ext

import cats.MonadThrow
import cats.syntax.all.*

import org.typelevel.log4cats.Logger

extension [F[_]: MonadThrow: Logger](logger: Logger[F])
  def protectInfo[A](pre: => String, post: => String)(effect: F[A]): F[A] =
    Logger[F].info(pre) *> effect <* Logger[F].info(post)

  def bracketProtectInfo[A](pre: => String, post: => String, onError: => String)(
      effect: F[A]
  ): F[A] =
    protectInfo(pre, post)(effect).onError(e => Logger[F].error(e)(onError))
