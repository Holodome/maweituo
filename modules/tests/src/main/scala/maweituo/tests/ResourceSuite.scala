package maweituo
package tests

import weaver.scalacheck.CheckConfig

abstract class ResourceSuite extends MaweituoSuite:

  override def maxParallelism: Int = 1
  // For it:tests, one is enough
  override def checkConfig: CheckConfig =
    CheckConfig.default.copy(minimumSuccessful = 1)
