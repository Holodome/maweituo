package maweituo
package tests

import weaver.scalacheck.CheckConfig

abstract class ResourceSuite extends MaweituoSuite:

  // For it:tests, one is enough
  override def checkConfig: CheckConfig =
    CheckConfig.default.copy(minimumSuccessful = 1, perPropertyParallelism = 1)
