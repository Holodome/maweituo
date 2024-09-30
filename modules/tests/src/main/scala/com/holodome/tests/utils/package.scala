package com.holodome.tests.utils

import com.holodome.domain.users.User

import cats.Show
import cats.derived.*

given Show[User] = Show.derived
