package com.holodome

import cats.effect.{ContextShift, IO}

import scala.concurrent.ExecutionContext

object AppContextShift {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
}
