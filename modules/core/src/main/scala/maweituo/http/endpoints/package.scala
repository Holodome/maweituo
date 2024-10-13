package maweituo
package http
package endpoints

private object AdEndpoints:
  export maweituo.http.endpoints.ads.AdImageEndpoints
  export maweituo.http.endpoints.ads.AdEndpoints
  export maweituo.http.endpoints.ads.AdTagEndpoints
  export maweituo.http.endpoints.ads.AdChatEndpoints
  export maweituo.http.endpoints.ads.AdMsgEndpoints

private object UserEndpoints:
  export maweituo.http.endpoints.users.UserAdEndpoints
  export maweituo.http.endpoints.users.UserEndpoints

object all:
  export AdEndpoints.*
  export UserEndpoints.*
  export maweituo.http.endpoints.AuthEndpoints
  export maweituo.http.endpoints.RegisterEndpoints
  export maweituo.http.endpoints.FeedEndpoints
  export maweituo.http.endpoints.TagEndpoints
