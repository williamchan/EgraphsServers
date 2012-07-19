package utils

class TestBeforeAndAfterClosuresFlat extends EgraphsUnitTest
  with BeforeAndAfterClosures
{
  var beforeExecutionCount = 0
  var afterExecutionCount = 0

  beforeEachTest {
    beforeExecutionCount += 1
  }

  afterEachTest {
    afterExecutionCount += 1
  }

  "Initial execution counts" should "be correct" in {
    beforeExecutionCount should be (1)
    afterExecutionCount should be (0)
  }

  "Second execution counts" should "be correct" in {
    beforeExecutionCount should be (2)
    afterExecutionCount should be (1)
  }
}

trait IncrementsExecutionCounts { this: BeforeAndAfterClosures =>
  var beforesExecuted = 0
  var aftersExecuted = 0
  var lastBeforeExecuted = ""
  var lastAfterExecuted = ""

  beforeEachTest {
    beforesExecuted += 1
    lastBeforeExecuted = "trait"
  }

  afterEachTest {
    aftersExecuted += 1
    lastAfterExecuted = "trait"
  }
}

class TestBeforeAndAfterClosuresMixedIn extends EgraphsUnitTest
  with BeforeAndAfterClosures
  with IncrementsExecutionCounts
{

  beforeEachTest {
    lastBeforeExecuted = "class"
  }

  afterEachTest {
    // Incrementing by 1 here and by 1 in IncrementsExecutionCounts
    // means that aftersExecuted should rise by 2 with each test case
    aftersExecuted += 1
    lastBeforeExecuted = "class"
  }

  "Initial execution data" should "be correct" in {
    this.beforesExecuted should be (1)
    this.aftersExecuted should be (0)

    this.lastBeforeExecuted should be ("class")
  }

  "Second execution data" should "be correct" in {
    this.beforesExecuted should be (2)
    this.aftersExecuted should be (2)

    this.lastAfterExecuted should be ("trait")
  }
}