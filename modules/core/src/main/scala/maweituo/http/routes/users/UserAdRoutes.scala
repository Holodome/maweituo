package maweituo
package http
package routes
package users

import cats.effect.Concurrent
import cats.syntax.all.*

import maweituo.domain.all.*

import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger

final class UserAdRoutes[F[_]: JsonDecoder: Logger: Concurrent](
    userAdsService: UserAdsService[F]
) extends Http4sDsl[F] with PublicRoutes[F]:

  override val routes = HttpRoutes.of {
    case GET -> Root / "users" / UserIdVar(userId) / "ads" =>
      userAdsService
        .getAds(userId)
        .map(UserAdsResponseDto(userId, _))
        .flatMap(Ok(_))
  }
