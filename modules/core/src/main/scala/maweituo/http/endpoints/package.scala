package maweituo
package http
package endpoints

private trait AdEndpoints:
  export maweituo.http.endpoints.ads.AdImageEndpoints
  export maweituo.http.endpoints.ads.AdEndpoints
  export maweituo.http.endpoints.ads.AdTagEndpoints
  export maweituo.http.endpoints.ads.AdChatEndpoints
  export maweituo.http.endpoints.ads.AdMsgEndpoints

private trait UserEndpoints:
  export maweituo.http.endpoints.users.UserAdEndpoints
  export maweituo.http.endpoints.users.UserChatEndpoints
  export maweituo.http.endpoints.users.UserEndpoints

object all extends AdEndpoints with UserEndpoints:
  export maweituo.http.endpoints.AuthEndpoints
  export maweituo.http.endpoints.RegisterEndpoints
  export maweituo.http.endpoints.FeedEndpoints
  export maweituo.http.endpoints.TagEndpoints
