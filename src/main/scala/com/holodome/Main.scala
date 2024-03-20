package com.holodome

import cats.effect.{ExitCode, IO, IOApp}

import scala.concurrent.ExecutionContext
import com.holodome.config.Config
import com.holodome.http.Routes
import com.holodome.repositories.UserRepository
import com.holodome.repositories.cassandra.cql.UsersDatabase
import com.outworkers.phantom.connectors.ContactPoints
import com.outworkers.phantom.dsl.CassandraConnection
import com.holodome.repositories.cassandra.CassandraUserRepository
import com.holodome.resources.MkHttpServer
import com.outworkers.phantom.connectors
import org.http4s.HttpApp
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.middleware.Logger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp.Simple {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  override def run: IO[Unit] = {
    val userRepo = new CassandraUserRepository(
      new UsersDatabase(connectors.ContactPoint.local.keySpace("aboba"))
    )
    val services = Services.make[IO](userRepo)
    val routes = new Routes[IO](services).routes
    val httpApp: HttpApp[IO] = routes.orNotFound
    MkHttpServer[IO].newEmber(httpApp).useForever
  }
}
