package maweituo.tests

class WeaverLogAdapter[F[_]](log: weaver.Log[F]) extends org.typelevel.log4cats.Logger[F]:

  override def error(message: => String): F[Unit] =
    log.error(message)

  override def warn(message: => String): F[Unit] =
    log.warn(message)

  override def info(message: => String): F[Unit] =
    log.info(message)

  override def debug(message: => String): F[Unit] =
    log.debug(message)

  override def trace(message: => String): F[Unit] =
    log.info(message)

  override def error(t: Throwable)(message: => String): F[Unit] =
    log.error(s"$t: $message")

  override def warn(t: Throwable)(message: => String): F[Unit] =
    log.warn(s"$t: $message")

  override def info(t: Throwable)(message: => String): F[Unit] =
    log.info(s"$t: $message")

  override def debug(t: Throwable)(message: => String): F[Unit] =
    log.debug(s"$t: $message")

  override def trace(t: Throwable)(message: => String): F[Unit] =
    log.info(s"$t: $message")
