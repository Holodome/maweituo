package maweituo.domain

import cats.Show
import cats.derived.derived

import maweituo.domain.users.UserId

final case class Identity(id: UserId) derives Show

object Identity:
  given Conversion[Identity, UserId] = x => x.id
