package maweituo
package domain
package repos

trait AdRepos:
  type AdImageRepo[F[_]]  = maweituo.domain.repos.ads.AdImageRepo[F]
  type AdRepo[F[_]]       = maweituo.domain.repos.ads.AdRepo[F]
  type AdSearchRepo[F[_]] = maweituo.domain.repos.ads.AdSearchRepo[F]
  type AdTagRepo[F[_]]    = maweituo.domain.repos.ads.AdTagRepo[F]
  type ChatRepo[F[_]]     = maweituo.domain.repos.ads.ChatRepo[F]
  type MessageRepo[F[_]]  = maweituo.domain.repos.ads.MessageRepo[F]

trait UserRepos:
  type UserRepo[F[_]] = maweituo.domain.repos.users.UserRepo[F]

trait Repos extends AdRepos with UserRepos:
  type RecsRepo[F[_]]      = maweituo.domain.repos.RecsRepo[F]
  type TelemetryRepo[F[_]] = maweituo.domain.repos.TelemetryRepo[F]

object all extends Repos
