package models

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import utils.{DBTransactionPerTest, TestData, EgraphsUnitTest}

@RunWith(classOf[JUnitRunner])
class EgraphVideoAssetTests extends EgraphsUnitTest with DBTransactionPerTest {

  "encodeVideo" should "return byte array of video-fied egraph" in new EgraphsTestApplication {
    val egraph = TestData.newSavedEgraphWithRealAudio()
    egraph.getVideoAsset.encodeVideo().length should be(52011)
  }
}
