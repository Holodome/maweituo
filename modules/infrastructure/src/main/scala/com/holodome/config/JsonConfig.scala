package com.holodome.config

import cats.effect.Sync
import cats.syntax.all._
import ciris.ConfigError
import ciris.ConfigKey
import ciris.ConfigValue
import io.circe.ACursor
import io.circe.Json

import java.nio.file.Files
import java.nio.file.Path

case class JsonConfig private (json: Json) {
  def stringField[F[_]](location: String): ConfigValue[F, String] =
    field[F, String](location)

  def intField[F[_]](location: String): ConfigValue[F, Int] =
    field[F, Int](location)

  def field[F[_], A: io.circe.Decoder](location: String): ConfigValue[F, A] = {
    val key                  = ConfigKey(s"json file field ${location}")
    def errorMessage: String = s"failed to get json field $location"

    val locParts        = location.split("\\.")
    val cursor: ACursor = json.hcursor
    val objectCursor    = locParts.foldLeft(cursor) { case (c, v) => c.downField(v) }
    objectCursor
      .as[A]
      .toOption
      .fold(
        ciris.ConfigValue.failed[A](ConfigError.sensitive(errorMessage, errorMessage))
      )(v => ciris.ConfigValue.loaded(key, v))
  }
}

object JsonConfig {
  def fromFile[F[_]: Sync](path: Path): F[JsonConfig] = {
    Sync[F]
      .blocking {
        Files.readString(path)
      }
      .flatMap { str =>
        io.circe.parser.parse(str).liftTo[F]
      }
      .map(JsonConfig.apply)
  }
}
