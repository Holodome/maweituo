package maweituo
package postgres
package repos

trait PostgresAdRepos:
  export maweituo.postgres.repos.ads.PostgresAdImageRepo
  export maweituo.postgres.repos.ads.PostgresAdRepo
  export maweituo.postgres.repos.ads.PostgresAdSearchRepo
  export maweituo.postgres.repos.ads.PostgresAdTagRepo
  export maweituo.postgres.repos.ads.PostgresChatRepo
  export maweituo.postgres.repos.ads.PostgresMessageRepo

trait PostgresUserRepos:
  export maweituo.postgres.repos.users.PostgresUserRepo

trait PostgresRepos extends PostgresAdRepos with PostgresUserRepos:
  export maweituo.postgres.repos.PostgresRecsRepo
  export maweituo.postgres.repos.PostgresTelemetryRepo

object all extends PostgresRepos
