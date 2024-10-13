package maweituo
package tests
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}

final class WeaverLogAdapter[F[_]: Applicative](log: weaver.Log[F]) extends SelfAwareStructuredLogger[F]:

  override def isTraceEnabled: F[Boolean] = Applicative[F].pure(true)
  override def isDebugEnabled: F[Boolean] = Applicative[F].pure(true)
  override def isInfoEnabled: F[Boolean]  = Applicative[F].pure(true)
  override def isWarnEnabled: F[Boolean]  = Applicative[F].pure(true)
  override def isErrorEnabled: F[Boolean] = Applicative[F].pure(true)

  override def error(message: => String): F[Unit] = log.error(message)
  override def warn(message: => String): F[Unit]  = log.warn(message)
  override def info(message: => String): F[Unit]  = log.info(message)
  override def debug(message: => String): F[Unit] = log.debug(message)
  override def trace(message: => String): F[Unit] = log.info(message)

  override def error(t: Throwable)(message: => String): F[Unit] = log.error(s"$t: $message")
  override def warn(t: Throwable)(message: => String): F[Unit]  = log.warn(s"$t: $message")
  override def info(t: Throwable)(message: => String): F[Unit]  = log.info(s"$t: $message")
  override def debug(t: Throwable)(message: => String): F[Unit] = log.debug(s"$t: $message")
  override def trace(t: Throwable)(message: => String): F[Unit] = log.info(s"$t: $message")

  override def trace(ctx: Map[String, String])(msg: => String): F[Unit]               = trace(msg)
  override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = trace(msg)
  override def debug(ctx: Map[String, String])(msg: => String): F[Unit]               = debug(msg)
  override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = debug(msg)
  override def info(ctx: Map[String, String])(msg: => String): F[Unit]                = info(msg)
  override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit]  = info(msg)
  override def warn(ctx: Map[String, String])(msg: => String): F[Unit]                = warn(msg)
  override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit]  = warn(msg)
  override def error(ctx: Map[String, String])(msg: => String): F[Unit]               = error(msg)
  override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = error(msg)

def const[F[_]](
    logger: SelfAwareStructuredLogger[F]
)(using F: Applicative[F]): LoggerFactory[F] = new LoggerFactory[F]:
  override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] = logger
  override def fromName(name: String): F[SelfAwareStructuredLogger[F]]       = F.pure(logger)

object WeaverLogAdapterFactory:
  def apply[F[_]: Applicative](log: weaver.Log[F]) = const(WeaverLogAdapter(log))
