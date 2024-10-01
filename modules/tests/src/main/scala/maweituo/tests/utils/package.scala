package maweituo.tests.utils

import cats.Show
import cats.derived.*

import maweituo.domain.users.User

given Show[User] = Show.derived
