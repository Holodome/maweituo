package maweituo.config.utils

import java.nio.file.{Files, Path}

import cats.effect.Sync
import cats.syntax.all.*
import ciris.{ConfigError, ConfigKey, ConfigValue}
import io.circe.{ACursor, Json}

case class JsonConfig private (json: Json):
  def stringField[F[_]](location: String): ConfigValue[F, String] =
    field[F, String](location)

  def intField[F[_]](location: String): ConfigValue[F, Int] =
    field[F, Int](location)

  def field[F[_], A: io.circe.Decoder](location: String): ConfigValue[F, A] =
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

object JsonConfig:
  def fromString[F[_]: Sync](str: String): F[JsonConfig] =
    io.circe.parser.parse(str).liftTo[F].map(JsonConfig.apply)

  def fromFile[F[_]: Sync](path: Path): F[JsonConfig] =
    Sync[F]
      .blocking {
        Files.readString(path)
      }
      .flatMap(fromString)
