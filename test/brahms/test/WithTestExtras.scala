package brahms.test

import play.api.test.FakeApplication

trait WithTestExtras {

  val fakeApplication = FakeApplication(additionalConfiguration = Map(
    "logger.application" -> "DEBUG"
  ) )
}
