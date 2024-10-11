package maweituo
package domain
package repos

trait RecsRepo[F[_]]:
  def learn: F[Unit]
