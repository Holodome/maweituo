package com.holodome.effects

import cats.syntax.all._
import cats.effect.Sync

import java.nio.file.{Path, Paths}

trait RelativeFile[F[_]] {
  def pwd: F[Path]
  def relativePath(path: Path): F[Path]
}

object RelativeFile {
  def apply[F[_]: RelativeFile]: RelativeFile[F] = implicitly

  implicit def forSync[F[_]: Sync]: RelativeFile[F] = new RelativeFile[F] {

    override def pwd: F[Path] = Sync[F].delay(Paths.get("").toAbsolutePath)

    override def relativePath(path: Path): F[Path] =
      pwd.flatMap(pwd => Sync[F].delay(pwd.relativize(path)))
  }
}
