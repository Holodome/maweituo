version := "0.1.0-SNAPSHOT"
scalaVersion := "2.13.11"
scalacOptions += "-Ymacro-annotations"
organization := "com.holodome"

lazy val root = (project in file("."))
  .settings(
    name := "backend"
  )

val CatsVersion = "2.9.0"
val CatsEffectVersion = "3.3.12"
val Log4CatsVersion = "2.6.0"
val Http4sVersion = "0.23.1"
val CirceVersion = "0.14.1"
val javaDriverVersion = "4.9.0"
val PhantomVersion = "2.59.0"
val DerevoVersion = "0.12.8"
val RefinedVersion = "0.11.1"

libraryDependencies += "org.typelevel" %% "cats-core" % CatsVersion
libraryDependencies += "org.typelevel" %% "cats-effect" % CatsEffectVersion
libraryDependencies += "org.typelevel" %% "log4cats-slf4j" % Log4CatsVersion
libraryDependencies += "org.http4s" %% "http4s-ember-server" % Http4sVersion
libraryDependencies += "org.http4s" %% "http4s-ember-client" % Http4sVersion
libraryDependencies += "org.http4s" %% "http4s-circe" % Http4sVersion
libraryDependencies += "org.http4s" %% "http4s-dsl" % Http4sVersion
libraryDependencies += "io.circe" %% "circe-generic" % CirceVersion
libraryDependencies += "io.circe" %% "circe-shapes" % CirceVersion
libraryDependencies += "io.circe" %% "circe-parser" % CirceVersion
libraryDependencies += "io.circe" %% "circe-generic-extras" % CirceVersion
libraryDependencies += "io.circe" %% "circe-derivation" % "0.13.0-M5"
libraryDependencies += "com.outworkers" %% "phantom-dsl" % PhantomVersion
libraryDependencies += "tf.tofu" %% "derevo-core" % DerevoVersion
libraryDependencies += "tf.tofu" %% "derevo-circe" % DerevoVersion
libraryDependencies += "tf.tofu" %% "derevo-cats" % DerevoVersion
libraryDependencies += "tf.tofu" %% "derevo-circe-magnolia" % DerevoVersion
libraryDependencies += "eu.timepit" %% "refined" % RefinedVersion
libraryDependencies += "eu.timepit" %% "refined-cats" % RefinedVersion