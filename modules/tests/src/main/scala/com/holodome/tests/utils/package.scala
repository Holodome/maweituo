package com.holodome.tests.utils

import cats.Show
import cats.derived.*
import com.holodome.domain.users.User

given Show[User] = Show.derived
