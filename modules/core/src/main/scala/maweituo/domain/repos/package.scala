package maweituo
package domain
package repos

trait AdRepos:
  export maweituo.domain.repos.ads.AdImageRepo
  export maweituo.domain.repos.ads.AdRepo
  export maweituo.domain.repos.ads.AdSearchRepo
  export maweituo.domain.repos.ads.AdTagRepo
  export maweituo.domain.repos.ads.ChatRepo
  export maweituo.domain.repos.ads.MessageRepo

trait UserRepos:
  export maweituo.domain.repos.users.UserRepo

trait Repos extends AdRepos with UserRepos:
  export maweituo.domain.repos.RecsRepo
  export maweituo.domain.repos.TelemetryRepo

object all extends Repos
