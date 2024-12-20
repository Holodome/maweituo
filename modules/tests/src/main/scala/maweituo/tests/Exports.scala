package maweituo
package tests

export maweituo.logic.errors.*
export maweituo.logic.interp.all.*
export maweituo.domain.all.*
export org.scalacheck.Gen
export generators.*
export cats.effect.IO
export cats.effect.Resource
export org.typelevel.log4cats.LoggerFactory
export weaver.scalacheck.Checkers
export weaver.{Expectations, SimpleIOSuite, SourceLocation}
export resources.MinioContainerResource
export resources.PostgresContainerResource
export resources.RedisContainerResource

export cats.Show
export cats.Monad
export cats.syntax.all.*
export cats.derived.derived
export cats.data.OptionT
export cats.data.NonEmptyList
export cats.Applicative
export cats.ApplicativeThrow
export cats.Functor
export cats.MonadThrow
export cats.effect.Async
export cats.effect.Sync

trait OrphanInstances:
  import maweituo.domain.all.*
  import dev.profunktor.auth.jwt.JwtToken
  import maweituo.utils.given

  given Show[User]                  = Show.derived
  given Show[LoginRequest]          = Show.derived
  given Show[RegisterRequest]       = Show.derived
  given Show[UpdateUserRequest]     = Show.derived
  given Show[UpdateUserRepoRequest] = Show.derived
  given Show[JwtToken]              = Show[String].contramap[JwtToken](_.value)

private object exports extends OrphanInstances
export exports.{*, given}
