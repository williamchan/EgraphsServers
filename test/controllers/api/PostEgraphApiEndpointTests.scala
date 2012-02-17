package controllers.api

import utils.{DBTransactionPerTest, EgraphsUnitTest}


class PostEgraphApiEndpointTests extends EgraphsUnitTest with DBTransactionPerTest {
  "PostEgraphApiEndpoint" should "save an egraph with status Verified if it skips biometrics" in {
    /*
    val response = new Controller with PostEgraphApiEndpoint {
      override implicit def validationErrors = Map.empty

      def celebFilters = friendlyFilters()
      def orderFilters = mock[OrderRequestFilters]
    }*/
    // TODO(erem): write this test
  }
}