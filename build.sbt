import sbt.Compile
import sbt.Keys.libraryDependencies
import sbtprotoc.ProtocPlugin.autoImport.PB

ThisBuild / scalaVersion := "2.13.11"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.holodome"

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-Ymacro-annotations",
    "-feature",
    "-language:implicitConversions",
    "-deprecation",
    "-Wunused:imports"
  )
)

val CatsVersion            = "2.9.0"
val CatsEffectVersion      = "3.3.12"
val Log4CatsVersion        = "2.6.0"
val Http4sVersion          = "0.23.1"
val CirceVersion           = "0.14.1"
val DerevoVersion          = "0.12.8"
val RefinedVersion         = "0.11.1"
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
val MockitoVersion         = "1.17.30"
val MeowMtlVersion         = "0.5.0"
val DoobieVersion          = "1.0.0-RC4"
val ClickhouseVersion      = "0.6.0"
val HikariCPVersion        = "5.1.0"
val LZ4Version             = "1.8.0"

lazy val root = (project in file("."))
  .settings(
    name := "maweituo"
  )
  .aggregate(coreHttp, tests, it, recs)

lazy val infrastructure = (project in file("modules/infrastructure"))
  .settings(
    commonSettings,
    name := "maweituo-infrastructure",
    libraryDependencies ++= Seq(
      "org.typelevel"  %% "cats-core"             % CatsVersion,
      "org.typelevel"  %% "cats-effect"           % CatsEffectVersion,
      "org.typelevel"  %% "log4cats-slf4j"        % Log4CatsVersion,
      "dev.profunktor" %% "redis4cats-effects"    % Redis4CatsVersion,
      "dev.profunktor" %% "redis4cats-log4cats"   % Redis4CatsVersion,
      "tf.tofu"        %% "derevo-core"           % DerevoVersion,
      "tf.tofu"        %% "derevo-circe"          % DerevoVersion,
      "tf.tofu"        %% "derevo-cats"           % DerevoVersion,
      "tf.tofu"        %% "derevo-circe-magnolia" % DerevoVersion,
      "io.minio"        % "minio"                 % MinioVersion,
      "eu.timepit"     %% "refined"               % RefinedVersion,
      "eu.timepit"     %% "refined-cats"          % RefinedVersion,
      "dev.optics"     %% "monocle-core"          % MonocleVersion,
      "dev.optics"     %% "monocle-macro"         % MonocleVersion,
      "is.cir"         %% "ciris"                 % CirisVersion,
      "is.cir"         %% "ciris-refined"         % CirisVersion,
      "is.cir"         %% "ciris-http4s"          % CirisVersion,
      "io.circe"       %% "circe-generic"         % CirceVersion,
      "io.circe"       %% "circe-shapes"          % CirceVersion,
      "io.circe"       %% "circe-parser"          % CirceVersion,
      "io.circe"       %% "circe-generic-extras"  % CirceVersion,
      "io.circe"       %% "circe-derivation"      % CirceDerivationVersion,
      "io.circe"       %% "circe-refined"         % CirceVersion,
      "io.estatico"    %% "newtype"               % NewtypeVersion,
      "dev.profunktor" %% "http4s-jwt-auth"       % Http4sJwtAuthVersion,
      "org.http4s"     %% "http4s-ember-server"   % Http4sVersion,
      "org.http4s"     %% "http4s-ember-client"   % Http4sVersion,
      "org.http4s"     %% "http4s-circe"          % Http4sVersion,
      "org.http4s"     %% "http4s-dsl"            % Http4sVersion
    )
  )

lazy val domain = (project in file("modules/domain"))
  .dependsOn(infrastructure)
  .settings(
    commonSettings,
    name := "maweituo-domain"
  )

lazy val common = (project in file("modules/common"))
  .dependsOn(domain)
  .settings(
    commonSettings,
    name := "maweituo-common",
    libraryDependencies ++= Seq(
      "com.zaxxer"       % "HikariCP"        % HikariCPVersion,
      "com.clickhouse"   % "clickhouse-jdbc" % ClickhouseVersion,
      "org.lz4"          % "lz4-java"        % LZ4Version,
      "org.tpolecat"    %% "doobie-core"     % DoobieVersion,
      "org.tpolecat"    %% "doobie-hikari"   % DoobieVersion,
      "com.ringcentral" %% "cassandra4io"    % Cassandra4IoVersion
    )
  )

lazy val cassandra = (project in file("modules/cassandra-da"))
  .dependsOn(domain)
  .settings(
    commonSettings,
    name := "maweituo-cassandra-da",
    libraryDependencies ++= Seq(
      "com.ringcentral" %% "cassandra4io" % Cassandra4IoVersion
    )
  )

lazy val grpc = (project in file("modules/grpc"))
  .dependsOn(domain)
  .enablePlugins(Http4sGrpcPlugin)
  .settings(
    commonSettings,
    name := "maweituo-grpc",
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime"      % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "com.thesamet.scalapb" %% "compilerplugin"       % "0.11.11",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
    ),
    Compile / PB.protoSources += file("proto"),
    Compile / PB.targets ++= Seq(
      scalapb.gen(grpc = false) -> (Compile / sourceManaged).value / "scalapb"
    )
  )

lazy val core = (project in file("modules/core"))
  .dependsOn(cassandra, grpc, common)
  .settings(
    commonSettings,
    name := "maweituo-core"
  )

lazy val coreHttp = (project in file("modules/core-http"))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .dependsOn(core)
  .settings(
    commonSettings,
    name := "maweituo-core-http",
    Compile / run / fork := true,
    scalafmtOnCompile := true,
    dockerExposedPorts ++= Seq(8080),
    dockerBaseImage := "openjdk:11-jre-slim-buster",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "com.olegpy"    %% "meow-mtl-core"   % MeowMtlVersion
    )
  )

lazy val tests = (project in file("modules/tests"))
  .dependsOn(coreHttp)
  .settings(
    commonSettings,
    name := "maweituo-tests",
    publish / skip := true,
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    libraryDependencies ++= Seq(
      "com.disneystreaming" %% "weaver-cats"        % WeaverVersion,
      "com.disneystreaming" %% "weaver-discipline"  % WeaverVersion,
      "com.disneystreaming" %% "weaver-scalacheck"  % WeaverVersion,
      "org.typelevel"       %% "log4cats-noop"      % Log4CatsVersion,
      "org.typelevel"       %% "cats-laws"          % CatsVersion,
      "dev.optics"          %% "monocle-law"        % MonocleVersion,
      "eu.timepit"          %% "refined-scalacheck" % RefinedVersion,
      "org.mockito"         %% "mockito-scala-cats" % MockitoVersion
    )
  )

lazy val it = (project in file("modules/it"))
  .dependsOn(tests)
  .settings(
    commonSettings,
    name := "maweituo-it",
    publish / skip := true
  )

lazy val recsDomain = (project in file("modules/recs-domain"))
  .dependsOn(domain)
  .settings(
    commonSettings,
    name := "maweituo-recs-domain"
  )

lazy val recsCassandra = (project in file("modules/recs-cassandra-da"))
  .dependsOn(recsDomain, cassandra)
  .settings(
    commonSettings,
    name := "maweituo-recs-cassandra-da"
  )

lazy val recsClickhouse = (project in file("modules/recs-clickhouse-da"))
  .dependsOn(recsDomain)
  .settings(
    commonSettings,
    name := "maweituo-recs-clickhouse-da",
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core" % DoobieVersion
    )
  )

lazy val recs = (project in file("modules/recs"))
  .dependsOn(recsDomain, recsCassandra, recsClickhouse, common, grpc)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    commonSettings,
    name := "maweituo-recs",
    Compile / run / fork := true,
    dockerExposedPorts ++= Seq(11223),
    dockerBaseImage := "openjdk:11-jre-slim-buster"
  )
