package maweituo
package tests

export maweituo.logic.errors.*
export maweituo.logic.interp.all.*
export maweituo.domain.all.*
export org.scalacheck.Gen
export generators.*
export cats.effect.IO
export cats.effect.Resource
export weaver.scalacheck.Checkers
export weaver.{Expectations, SimpleIOSuite, SourceLocation}
export resources.MinioContainerResource
export resources.PostgresContainerResource
export resources.RedisContainerResource

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
