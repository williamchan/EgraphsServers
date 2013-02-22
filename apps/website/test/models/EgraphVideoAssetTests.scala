package models

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import utils.{DBTransactionPerTest, TestData, EgraphsUnitTest}

@RunWith(classOf[JUnitRunner])
class EgraphVideoAssetTests extends EgraphsUnitTest with DBTransactionPerTest {

  "encodeVideo" should "return byte array of video-fied egraph" in new EgraphsTestApplication {
    val egraph = TestData.newSavedEgraphWithRealAudio()
    val egraphVideoLength = egraph.getVideoAsset.encodeVideo().length

    // Xuggle calls native code libraries that are different for various operating systems.
    // 52011 is expected on Mac, 52209 on Linux
    List(52011, 52209) should contain(egraphVideoLength)
  }
}
