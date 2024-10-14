package maweituo
package postgres

import java.util.UUID

import maweituo.utils.Wrapper

import doobie.util.meta.Meta

object codecs:
  given Meta[UUID] = Meta[String].imap[UUID](UUID.fromString)(_.toString)

  given [A, B](using wp: Wrapper[A, B], m: Meta[A]): Meta[B] =
    m.timap(wp.iso.get)(wp.iso.reverseGet)
