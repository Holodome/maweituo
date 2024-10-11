package maweituo
package infrastructure
package effects

import java.time.Instant
import java.time.temporal.ChronoUnit

trait TimeSource[F[_]]:
  def instant: F[Instant]

object TimeSource:
  def apply[F[_]: TimeSource]: TimeSource[F] = summon

  given [F[_]: Sync]: TimeSource[F] with
    def instant: F[Instant] = Sync[F].delay(Instant.now().truncatedTo(ChronoUnit.MICROS))
