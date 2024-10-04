package maweituo.it.resources

import weaver.GlobalResource

object RedisContainerResource    extends GlobalResource with maweituo.tests.resources.RedisContainerResource
object PostgresContainerResource extends GlobalResource with maweituo.tests.resources.PostgresContainerResource
object MinioContainerResource    extends GlobalResource with maweituo.tests.resources.MinioContainerResource
