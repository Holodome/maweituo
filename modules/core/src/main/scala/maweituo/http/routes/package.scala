package maweituo
package http
package routes

private object AdRoutes:
  export maweituo.http.routes.ads.AdImageRoutes
  export maweituo.http.routes.ads.AdRoutes
  export maweituo.http.routes.ads.AdTagRoutes
  export maweituo.http.routes.ads.AdChatRoutes
  export maweituo.http.routes.ads.AdMsgRoutes

private object UserRoutes:
  export maweituo.http.routes.users.UserAdRoutes
  export maweituo.http.routes.users.UserRoutes

object all:
  export AdRoutes.*
  export UserRoutes.*
  export maweituo.http.routes.LoginRoutes
  export maweituo.http.routes.LogoutRoutes
  export maweituo.http.routes.RegisterRoutes
  export maweituo.http.routes.FeedRoutes
  export maweituo.http.routes.TagRoutes
