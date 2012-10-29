package utils

import org.scalatest.{Suite, BeforeAndAfterEach}

/**
 * Extends BeforeAndAfterEach, allowing arbitrary number of unrelated tasks
 * to occur before and after each test case. Also allows you to abstract
 * the tasks into re-usable traits.
 *
 * Usage in the simplest case:
 * <code>
 *   class MyTestClass extends UnitFlatSpec with BeforeAndAfterClosures {
 *     beforeEachTest {
 *       setUpSomeThings()
 *     }
 *
 *     afterEachTest {
 *       tearDownSomeThings()
 *     }
 *
 *     // then write your test cases
 *   }
 * </code>
 *
 * More interestingly, as a trait:
 * <code>
 *   trait ClearsDatabaseAfter { this: BeforeAndAfterClosures =>
 *     afterEachTest {
 *       clearTheDatabase()
 *     }
 *   }
 *
 *   class MyDatabaseTestClass extends UnitFlatSpec
 *     with BeforeAndAfterClosures
 *     with ClearsDatabaseAfter
 *   {
 *     afterEachTest {
 *       // Both this (first) and the afterEachTest from the trait (second) get
 *       // executed
 *       doSomeBusinessSpecificToThisTestClass()
 *     }
 *   }
 * </code>
 */
trait BeforeAndAfterClosures extends BeforeAndAfterEach { this: Suite =>
  private var toDoBefore = List.empty[()=> Any]
  private var toDoAfter = List.empty[()=> Any]

  /**
   * Add a new task to be done before each test. Tasks will be executed
   * in reverse the order they were declared, meaning tasks from a mixed in trait
   * are called FIRST.
   *
   * Enables syntax like:
   * <code>
   *   beforeEachTest {
   *     doSomeSetup()
   *   }
   * </code>
   *
   */
  protected def beforeEachTest (newBeforeTask: => Any) {
    toDoBefore = toDoBefore ::: List(() => newBeforeTask)
  }

  /**
   * Add a new task to be done after each test. Tasks will be executed
   * in the order they were declared, meaning tasks from a mixed in trait
   * are called LAST.
   *
   * Enables syntax like:
   * <code>
   *   afterEachTest {
   *     doSomeCleanup()
   *   }
   * </code>
   */
  protected def afterEachTest (newAfterTask: => Any) {
    toDoAfter ::= (() => newAfterTask)
  }

  override final protected def beforeEach() {
    toDoBefore.foreach(task => task.apply())
  }

  override final protected def afterEach() {
    toDoAfter.foreach(task => task.apply())
  }
}
