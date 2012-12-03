package collections

import scala.collection.mutable.Queue

class LimitedQueue[A](limit:Int) extends Queue[A] {
  
  override def enqueue(elems: A*):Unit = {
    if (size >= limit) {
      dequeue()
    }
    super.enqueue(elems: _*)
  }
}
