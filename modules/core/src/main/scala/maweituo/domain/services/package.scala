package maweituo
package domain
package services

trait UserServices:
  type AuthService[F[_]]    = maweituo.domain.services.users.AuthService[F]
  type UserAdsService[F[_]] = maweituo.domain.services.users.UserAdsService[F]
  type UserService[F[_]]    = maweituo.domain.services.users.UserService[F]

trait AdServices:
  type AdImageService[F[_]] = maweituo.domain.services.ads.AdImageService[F]
  type AdService[F[_]]      = maweituo.domain.services.ads.AdService[F]
  type AdTagService[F[_]]   = maweituo.domain.services.ads.AdTagService[F]
  type ChatService[F[_]]    = maweituo.domain.services.ads.ChatService[F]
  type MessageService[F[_]] = maweituo.domain.services.ads.MessageService[F]

trait Services extends UserServices with AdServices:
  type FeedService[F[_]]      = maweituo.domain.services.FeedService[F]
  type IAMService[F[_]]       = maweituo.domain.services.IAMService[F]
  type TelemetryService[F[_]] = maweituo.domain.services.TelemetryService[F]

object all extends Services
