package com.holodome.models

sealed trait AppError

final case class LoginError(str: String) extends AppError