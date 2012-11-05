package services.http.filters

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.status
import services.config.ConfigFileProxy
import services.http.filters.FilterTestUtil.EitherErrorOrSuccess2RichErrorOrSuccess
import utils.EgraphsUnitTest

@RunWith(classOf[JUnitRunner])
class RequireApplicationIdTests extends EgraphsUnitTest {
  "filter" should "allow applicationIds that match that of the config" in {
    val filter = filterWithMocks { config =>
      config.applicationId returns "test"
    }

    val errorOrNothing = filter.filter("test")

    errorOrNothing should be(Right())
  }

  it should "not allow applicationIds that do not match that of the config" in {
    val filter = filterWithMocks { config =>
      config.applicationId returns "live"
    }

    val errorOrNothing = filter.filter("test")
    val result = errorOrNothing.toErrorOrOkResult

    status(result) should be(NOT_FOUND)
  }

  private def filterWithMocks(mockSetup: ConfigFileProxy => Unit): RequireApplicationId = {
    val config = mockConfigFileProxy {
      mockSetup
    }

    new RequireApplicationId(config)
  }

  private def mockConfigFileProxy(mockSetup: ConfigFileProxy => Unit): ConfigFileProxy = {
    val config = mock[ConfigFileProxy]
    mockSetup(config)

    config
  }
}