import sbt.Compile
import sbt.Keys.libraryDependencies
import sbtprotoc.ProtocPlugin.autoImport.PB

ThisBuild / scalaVersion := "3.4.0"
ThisBuild / version      := "0.1.0"
ThisBuild / organization := "maweituo"

autoCompilerPlugins := true

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-feature",
    "-language:implicitConversions",
    "-deprecation",
    "-Wunused:imports",
    "-Ykind-projector:underscores",
    "-Wunused:all"
  )
)

val CatsVersion            = "2.9.0"
val CatsEffectVersion      = "3.3.12"
val Log4CatsVersion        = "2.6.0"
val Http4sVersion          = "0.23.26"
val CirceVersion           = "0.14.1"
val DerevoVersion          = "0.12.8"
val CirisVersion           = "3.5.0"
val NewtypeVersion         = "0.4.4"
val LogbackVersion         = "1.4.14"
val MonocleVersion         = "3.1.0"
val Http4sJwtAuthVersion   = "1.2.0"
val Fs2Version             = "3.1.3"
val Redis4CatsVersion      = "1.1.1"
val CirceDerivationVersion = "0.13.0-M5"
val WeaverVersion          = "0.8.4"
val MinioVersion           = "8.5.9"
val MockitoVersion         = "1.17.30"
val MeowMtlVersion         = "0.5.0"
val DoobieVersion          = "1.0.0-RC4"
val HikariCPVersion        = "5.1.0"
val LZ4Version             = "1.8.0"
val IronVersion            = "2.6.0"
val KittensVersion         = "3.3.0"
val H2Version              = "2.3.230"
val TestcontainersVersion  = "0.41.4"
val SttpVersion            = "1.11.7"

lazy val root = (project in file("."))
  .settings(
    name := "maweituo"
  )
  .aggregate(core, tests, it, e2e)

lazy val core = (project in file("modules/core"))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    commonSettings,
    name                 := "maweituo-core",
    Compile / run / fork := true,
    scalafmtOnCompile    := true,
    dockerExposedPorts ++= Seq(8080),
    dockerBaseImage := "openjdk:11-jre-slim-buster",
    libraryDependencies ++= Seq(
      "com.h2database"               % "h2"                        % H2Version,
      "ch.qos.logback"               % "logback-classic"           % LogbackVersion,
      "org.typelevel"               %% "cats-core"                 % CatsVersion,
      "org.typelevel"               %% "cats-effect"               % CatsEffectVersion,
      "org.typelevel"               %% "log4cats-slf4j"            % Log4CatsVersion,
      "dev.optics"                  %% "monocle-core"              % MonocleVersion,
      "dev.optics"                  %% "monocle-macro"             % MonocleVersion,
      "is.cir"                      %% "ciris"                     % CirisVersion,
      "is.cir"                      %% "ciris-http4s"              % CirisVersion,
      "io.circe"                    %% "circe-parser"              % CirceVersion,
      "dev.profunktor"              %% "http4s-jwt-auth"           % Http4sJwtAuthVersion,
      "org.http4s"                  %% "http4s-ember-server"       % Http4sVersion,
      "org.http4s"                  %% "http4s-ember-client"       % Http4sVersion,
      "org.http4s"                  %% "http4s-circe"              % Http4sVersion,
      "org.http4s"                  %% "http4s-prometheus-metrics" % "0.24.6",
      "com.zaxxer"                   % "HikariCP"                  % HikariCPVersion,
      "org.lz4"                      % "lz4-java"                  % LZ4Version,
      "org.tpolecat"                %% "doobie-core"               % DoobieVersion,
      "org.tpolecat"                %% "doobie-hikari"             % DoobieVersion,
      "org.tpolecat"                %% "doobie-postgres"           % DoobieVersion,
      "dev.profunktor"              %% "redis4cats-effects"        % Redis4CatsVersion,
      "dev.profunktor"              %% "redis4cats-log4cats"       % Redis4CatsVersion,
      "io.minio"                     % "minio"                     % MinioVersion,
      "io.github.iltotore"          %% "iron"                      % IronVersion,
      "org.typelevel"               %% "kittens"                   % KittensVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-cats"                % SttpVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"          % SttpVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"       % SttpVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle"   % SttpVersion
    )
  )

lazy val tests = (project in file("modules/tests"))
  .dependsOn(core)
  .settings(
    commonSettings,
    name           := "maweituo-tests",
    publish / skip := true,
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    libraryDependencies ++= Seq(
      "io.circe"                    %% "circe-testing"                   % CirceVersion,
      "com.disneystreaming"         %% "weaver-cats"                     % WeaverVersion,
      "com.disneystreaming"         %% "weaver-discipline"               % WeaverVersion,
      "com.disneystreaming"         %% "weaver-scalacheck"               % WeaverVersion,
      "org.typelevel"               %% "log4cats-noop"                   % Log4CatsVersion,
      "org.typelevel"               %% "cats-laws"                       % CatsVersion,
      "com.dimafeng"                %% "testcontainers-scala"            % TestcontainersVersion,
      "com.dimafeng"                %% "testcontainers-scala-redis"      % TestcontainersVersion,
      "com.dimafeng"                %% "testcontainers-scala-postgresql" % TestcontainersVersion,
      "com.dimafeng"                %% "testcontainers-scala-minio"      % TestcontainersVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-client"             % SttpVersion
    )
  )

lazy val coreZio = (project in file("modules/core-zio"))
  .dependsOn(core)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    commonSettings,
    name                 := "maweituo-core-zio",
    Compile / run / fork := true,
    scalafmtOnCompile    := true,
    dockerExposedPorts ++= Seq(8081),
    dockerBaseImage := "openjdk:11-jre-slim-buster",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"              % "2.1.12",
      "dev.zio" %% "zio-streams"      % "2.1.12",
      "dev.zio" %% "zio-interop-cats" % "23.1.0.0"
    )
  )

lazy val it = (project in file("modules/it"))
  .dependsOn(tests)
  .settings(
    commonSettings,
    name           := "maweituo-it",
    publish / skip := true
  )

lazy val e2e = (project in file("modules/e2e"))
  .dependsOn(tests)
  .settings(
    commonSettings,
    name           := "maweituo-e2e",
    publish / skip := true
  )

lazy val fa2 = (project in file("modules/fa2"))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    commonSettings,
    name                 := "maweituo-fa2",
    Compile / run / fork := true,
    scalafmtOnCompile    := true,
    dockerExposedPorts ++= Seq(8080),
    dockerBaseImage := "openjdk:11-jre-slim-buster",
    libraryDependencies ++= Seq(
      "ch.qos.logback"      % "logback-classic"     % LogbackVersion,
      "org.typelevel"      %% "cats-core"           % CatsVersion,
      "org.typelevel"      %% "cats-effect"         % CatsEffectVersion,
      "org.typelevel"      %% "log4cats-slf4j"      % Log4CatsVersion,
      "dev.optics"         %% "monocle-core"        % MonocleVersion,
      "dev.optics"         %% "monocle-macro"       % MonocleVersion,
      "is.cir"             %% "ciris"               % CirisVersion,
      "is.cir"             %% "ciris-http4s"        % CirisVersion,
      "io.circe"           %% "circe-parser"        % CirceVersion,
      "org.http4s"         %% "http4s-ember-server" % Http4sVersion,
      "org.http4s"         %% "http4s-ember-client" % Http4sVersion,
      "org.http4s"         %% "http4s-circe"        % Http4sVersion,
      "org.http4s"         %% "http4s-dsl"          % Http4sVersion,
      "com.zaxxer"          % "HikariCP"            % HikariCPVersion,
      "org.lz4"             % "lz4-java"            % LZ4Version,
      "io.github.iltotore" %% "iron"                % IronVersion,
      "org.typelevel"      %% "kittens"             % KittensVersion,
      "com.sun.mail"        % "javax.mail"          % "1.5.6",
      "io.cucumber"        %% "cucumber-scala"      % "7.1.0" % Test,
      "io.cucumber"         % "cucumber-junit"      % "7.1.0" % Test,
      "junit"               % "junit"               % "4.13.2" % Test,
      "com.novocode"        % "junit-interface"     % "0.11"   % Test
    )
  )

inThisBuild(
  List(
    scalaVersion      := "3.4.0",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)
