package maweituo
package infrastructure
package effects

import java.time.Clock

import cats.effect.Sync
trait JwtClock[F[_]]:
  def utc: F[Clock]

object JwtClock:
  def apply[F[_]: JwtClock]: JwtClock[F] = summon

  given [F[_]: Sync]: JwtClock[F] with
    def utc: F[Clock] = Sync[F].delay(Clock.systemUTC())
