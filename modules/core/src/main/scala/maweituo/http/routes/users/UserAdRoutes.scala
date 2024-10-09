package maweituo.http.routes.users

import cats.effect.Concurrent
import cats.syntax.all.*

import maweituo.domain.users.*
import maweituo.domain.users.services.UserAdsService
import maweituo.http.PublicRoutes
import maweituo.http.dto.UserAdsResponseDto
import maweituo.http.vars.UserIdVar

import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger

final case class UserAdRoutes[F[_]: JsonDecoder: Logger: Concurrent](
    userAdsService: UserAdsService[F]
) extends Http4sDsl[F] with PublicRoutes[F]:

  override val routes = HttpRoutes.of {
    case GET -> Root / "users" / UserIdVar(userId) / "ads" =>
      userAdsService
        .getAds(userId)
        .map(UserAdsResponseDto(userId, _))
        .flatMap(Ok(_))
  }
