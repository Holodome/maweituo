import sbt.Keys.libraryDependencies

ThisBuild / scalaVersion := "2.13.11"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.holodome"

val CatsVersion            = "2.9.0"
val CatsEffectVersion      = "3.3.12"
val Log4CatsVersion        = "2.6.0"
val Http4sVersion          = "0.23.1"
val CirceVersion           = "0.14.1"
val DerevoVersion          = "0.12.8"
val RefinedVersion         = "0.11.1"
val EnumeratumVersion      = "1.7.3"
val CirisVersion           = "3.5.0"
val NewtypeVersion         = "0.4.4"
val LogbackVersion         = "1.4.14"
val MonocleVersion         = "3.1.0"
val Http4sJwtAuthVersion   = "1.0.0"
val Fs2Version             = "3.1.3"
val Redis4CatsVersion      = "1.1.1"
val CirceDerivationVersion = "0.13.0-M5"
val WeaverVersion          = "0.8.4"
val MinioVersion           = "8.5.9"
val Cassandra4IoVersion    = "0.1.14"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    name := "maweituo",
    scalafmtOnCompile := true,
    dockerExposedPorts ++= Seq(8080),
    dockerBaseImage := "openjdk:11-jre-slim-buster",
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    libraryDependencies ++= Seq(
      "org.typelevel"       %% "cats-core"             % CatsVersion,
      "org.typelevel"       %% "cats-effect"           % CatsEffectVersion,
      "org.typelevel"       %% "log4cats-slf4j"        % Log4CatsVersion,
      "org.http4s"          %% "http4s-ember-server"   % Http4sVersion,
      "org.http4s"          %% "http4s-ember-client"   % Http4sVersion,
      "org.http4s"          %% "http4s-circe"          % Http4sVersion,
      "org.http4s"          %% "http4s-dsl"            % Http4sVersion,
      "io.circe"            %% "circe-generic"         % CirceVersion,
      "io.circe"            %% "circe-shapes"          % CirceVersion,
      "io.circe"            %% "circe-parser"          % CirceVersion,
      "io.circe"            %% "circe-generic-extras"  % CirceVersion,
      "io.circe"            %% "circe-derivation"      % CirceDerivationVersion,
      "io.circe"            %% "circe-refined"         % CirceVersion,
      "tf.tofu"             %% "derevo-core"           % DerevoVersion,
      "tf.tofu"             %% "derevo-circe"          % DerevoVersion,
      "tf.tofu"             %% "derevo-cats"           % DerevoVersion,
      "tf.tofu"             %% "derevo-circe-magnolia" % DerevoVersion,
      "eu.timepit"          %% "refined"               % RefinedVersion,
      "eu.timepit"          %% "refined-cats"          % RefinedVersion,
      "com.beachape"        %% "enumeratum"            % EnumeratumVersion,
      "is.cir"              %% "ciris"                 % CirisVersion,
      "is.cir"              %% "ciris-enumeratum"      % CirisVersion,
      "is.cir"              %% "ciris-refined"         % CirisVersion,
      "io.estatico"         %% "newtype"               % NewtypeVersion,
      "ch.qos.logback"       % "logback-classic"       % LogbackVersion,
      "dev.optics"          %% "monocle-core"          % MonocleVersion,
      "dev.optics"          %% "monocle-macro"         % MonocleVersion,
      "dev.profunktor"      %% "http4s-jwt-auth"       % Http4sJwtAuthVersion,
      "dev.profunktor"      %% "redis4cats-effects"    % Redis4CatsVersion,
      "dev.profunktor"      %% "redis4cats-log4cats"   % Redis4CatsVersion,
      "io.minio"             % "minio"                 % MinioVersion,
      "com.ringcentral"     %% "cassandra4io"          % Cassandra4IoVersion,
      "com.disneystreaming" %% "weaver-cats"           % WeaverVersion   % Test,
      "com.disneystreaming" %% "weaver-discipline"     % WeaverVersion   % Test,
      "com.disneystreaming" %% "weaver-scalacheck"     % WeaverVersion   % Test,
      "org.typelevel"       %% "log4cats-noop"         % Log4CatsVersion % Test,
      "org.typelevel"       %% "cats-laws"             % CatsVersion     % Test,
      "dev.optics"          %% "monocle-law"           % MonocleVersion  % Test,
      "eu.timepit"          %% "refined-scalacheck"    % RefinedVersion  % Test,
      "org.mockito"         %% "mockito-scala-cats"    % "1.17.30"       % Test
    )
  )

scalacOptions ++= Seq(
  "-Ymacro-annotations",
  "-feature",
  "-language:implicitConversions",
  "-deprecation"
)
