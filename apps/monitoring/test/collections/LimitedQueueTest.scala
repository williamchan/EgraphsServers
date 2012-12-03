package collections

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers

class LimitedQueueTest extends FlatSpec with ShouldMatchers {

  "A LimitedQueue" should "pop values in first-in-first-out order" in {
    val lqueue = new LimitedQueue[Int](5)
    lqueue.enqueue(1)
    lqueue.enqueue(2)
    lqueue.dequeue() should equal(1)
    lqueue.dequeue() should equal(2)
  }

  "A LimitedQueue" should "not allow more elements than the provided limit" in {
    val lqueue = new LimitedQueue[Int](2)
    lqueue.enqueue(4)
    lqueue.enqueue(9)
    lqueue.enqueue(2)
    lqueue should have size (2)
  }

  "A LimitedQueue" should "keep most recent elements in case of exceeding limit" in {
	val lqueue = new LimitedQueue[Int](2)
	lqueue.enqueue(1)
	lqueue.enqueue(2)
	lqueue.enqueue(3)
	lqueue.head should equal (2)
	lqueue.last should equal (3)
  }

  "A LimitedQueue" should "throw NoSuchElementException if an empty queue is dequeued" in {
    val emptyQueue = new LimitedQueue[String](5)
    evaluating { emptyQueue.dequeue() } should produce[NoSuchElementException]
  }
}