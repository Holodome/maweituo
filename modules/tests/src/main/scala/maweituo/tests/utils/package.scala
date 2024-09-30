package maweituo.tests.utils

import maweituo.domain.users.User

import cats.Show
import cats.derived.*

given Show[User] = Show.derived
