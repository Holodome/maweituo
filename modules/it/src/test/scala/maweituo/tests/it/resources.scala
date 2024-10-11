package maweituo
package tests
package it

import weaver.GlobalResource

object resources:
  
  object RedisContainerResource    extends GlobalResource with maweituo.tests.resources.RedisContainerResource
  object PostgresContainerResource extends GlobalResource with maweituo.tests.resources.PostgresContainerResource
  object MinioContainerResource    extends GlobalResource with maweituo.tests.resources.MinioContainerResource
