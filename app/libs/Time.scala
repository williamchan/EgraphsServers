package libs

import java.util.Date
import java.sql.Timestamp

/**
 * Convenience methods for dealing with time
 */
object Time {
  /** What should be the default timestamp for entities. Occurs at epoch 0 */
  def defaultTimestamp: Timestamp = {
    new Timestamp(0L)
  }
  
  /** The current time */
  def now:Timestamp = {
    new Timestamp(new Date().getTime)
  }
}