package maweituo.domain.repos

trait RecsRepo[F[_]]:
  def learn: F[Unit]
