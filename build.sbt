version := "0.1.0-SNAPSHOT"
scalaVersion := "2.13.11"
scalacOptions += "-Ymacro-annotations"
organization := "com.holodome"

lazy val root = (project in file("."))
  .settings(
    name := "backend"
  )

val CatsVersion = "2.9.0"
val CatsEffectVersion = "2.5.3"
val Http4sVersion = "0.22.7"
val CirceVersion = "0.14.1"
val LogbackVersion = "1.2.3"
val EnumeratumVersion = "1.6.1"
val DoobieVersion = "0.13.4"
val TapirVersion = "0.18.3"
val Fs2Version = "2.5.10"
val PureconfigVersion = "0.17.1"
val TofuVersion = "0.10.6"
val DerevoVersion = "0.12.8"
val QuicklensVersion = "1.6.1"
val JwtScalaVersion = "9.0.2"
val javaDriverVersion = "4.9.0"

libraryDependencies += "org.typelevel" %% "cats-core" % CatsVersion
libraryDependencies += "org.typelevel" %% "cats-effect" % CatsEffectVersion
libraryDependencies += "org.http4s" %% "http4s-blaze-server" % Http4sVersion
libraryDependencies += "org.http4s" %% "http4s-blaze-client" % Http4sVersion
libraryDependencies += "org.http4s" %% "http4s-circe" % Http4sVersion
libraryDependencies += "org.http4s" %% "http4s-dsl" % Http4sVersion
libraryDependencies += "org.http4s" %% "http4s-prometheus-metrics" % Http4sVersion
libraryDependencies += "io.circe" %% "circe-generic" % CirceVersion
libraryDependencies += "io.circe" %% "circe-shapes" % CirceVersion
libraryDependencies += "io.circe" %% "circe-parser" % CirceVersion
libraryDependencies += "io.circe" %% "circe-generic-extras" % CirceVersion
libraryDependencies += "io.circe" %% "circe-derivation" % "0.13.0-M5"
libraryDependencies += "ch.qos.logback" % "logback-classic" % LogbackVersion
libraryDependencies += "com.beachape" %% "enumeratum" % EnumeratumVersion
libraryDependencies += "com.beachape" %% "enumeratum-circe" % EnumeratumVersion
libraryDependencies += "com.github.pureconfig" %% "pureconfig" % PureconfigVersion
libraryDependencies += "co.fs2" %% "fs2-core" % Fs2Version
libraryDependencies += "co.fs2" %% "fs2-io" % Fs2Version
libraryDependencies += "com.softwaremill.sttp.tapir" %% "tapir-core" % TapirVersion
libraryDependencies += "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % TapirVersion
libraryDependencies += "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % TapirVersion
libraryDependencies += "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % TapirVersion
libraryDependencies += "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % TapirVersion
libraryDependencies += "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % TapirVersion
libraryDependencies += "com.softwaremill.sttp.tapir" %% "tapir-redoc-http4s" % TapirVersion
libraryDependencies += "tf.tofu" %% "tofu-core-ce2" % TofuVersion
libraryDependencies += "tf.tofu" %% "tofu-concurrent" % TofuVersion
libraryDependencies += "tf.tofu" %% "derevo-core" % DerevoVersion
libraryDependencies += "tf.tofu" %% "derevo-circe" % DerevoVersion
libraryDependencies += "com.softwaremill.quicklens" %% "quicklens" % QuicklensVersion
libraryDependencies += "com.github.jwt-scala" %% "jwt-circe" % JwtScalaVersion

libraryDependencies += "com.outworkers" %% "phantom-dsl" % "2.59.0"