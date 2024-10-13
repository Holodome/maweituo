package maweituo
package domain
package services

trait UserServices:
  export maweituo.domain.services.users.AuthService
  export maweituo.domain.services.users.UserAdsService
  export maweituo.domain.services.users.UserService
  export maweituo.domain.services.users.UserChatsService

trait AdServices:
  export maweituo.domain.services.ads.AdImageService
  export maweituo.domain.services.ads.AdService
  export maweituo.domain.services.ads.AdTagService
  export maweituo.domain.services.ads.ChatService
  export maweituo.domain.services.ads.MessageService

trait Services extends UserServices with AdServices:
  export maweituo.domain.services.FeedService
  export maweituo.domain.services.IAMService
  export maweituo.domain.services.TelemetryService

object all extends Services
