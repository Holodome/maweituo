package maweituo
package logic
package interp

private trait AdsInterp:
  export maweituo.logic.interp.ads.AdImageServiceInterp
  export maweituo.logic.interp.ads.AdServiceInterp
  export maweituo.logic.interp.ads.AdTagServiceInterp
  export maweituo.logic.interp.ads.ChatServiceInterp
  export maweituo.logic.interp.ads.MessageServiceInterp

private trait UserServiceIntrep:
  export maweituo.logic.interp.users.UserAdsServiceInterp
  export maweituo.logic.interp.users.UserChatsServiceInterp
  export maweituo.logic.interp.users.UserServiceInterp

object all extends AdsInterp with UserServiceIntrep:
  export maweituo.logic.interp.AuthServiceInterp
  export maweituo.logic.interp.FeedServiceInterp
  export maweituo.logic.interp.IAMServiceInterp
  export maweituo.logic.interp.TelemetryServiceBackgroundInterp
  export maweituo.logic.interp.TelemetryServiceInterp
