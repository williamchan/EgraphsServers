package services.logging

/**
 * Provides any class that mixes it in with easy logging functionality.
 */
trait Logging {

  import Logging._

  /**
   * The class name used in the logs is the final part of the fully qualified named. For example,
   * a log from "java.util.String" would turn into "String".
   */
  private lazy val shortClassName = {
    val fullName = this.getClass.getName.split("\\.").last

    if (fullName.length < MAX_CLASSNAME_SIZE) {
      fullName
    } else {
      fullName.substring(0, MAX_CLASSNAME_SIZE) + "\u2026"
    }
  }

  /**Logs a message to INFO by default */
  def log(message: => String) {
    play.Logger.info(annotateMessage(message))
  }

  def error(message: => String) {
    play.Logger.error(annotateMessage(message))
  }

  private def annotateMessage(message: String): String = {
    val sb = new StringBuilder

    colorize("[", TerminalColorLightGray, sb)
    colorize(shortClassName, TerminalColorLightGray, sb)
    colorize("]", TerminalColorLightGray, sb)
    sb.append(" ")
    colorize(message, TerminalColorDefaultBold, sb)

    sb.toString()
  }
}

object Logging {
  /**The maximum length a classname should occupy in the logs */
  val MAX_CLASSNAME_SIZE = 15

  val colorizer = BoringColorizer

  val colorize = colorizer.colorize _

  /**Describes classes that can colorize text and write the colorized text to a StringBuilder */
  trait Colorizer {
    def colorize(message: String, color: TerminalColor, sb: StringBuilder)
  }

  /**
   * Prints colorized logs
   */
  object AnsiColorizer extends Colorizer {
    def colorize(message: String, color: TerminalColor, sb: StringBuilder) {
      sb.append(color.ansiVal)
      sb.append(message)
      sb.append(TerminalColorDefault.ansiVal)
    }
  }

  /**
   * Prints drab, uncolored logs
   */
  object BoringColorizer extends Colorizer {
    def colorize(message: String, color: TerminalColor, sb: StringBuilder) {
      sb.append(message)
    }
  }

  private[Logging] abstract class TerminalColor(val ansiVal: String)

  case object TerminalColorDefault extends TerminalColor("\u001B[00m")

  case object TerminalColorDefaultBold extends TerminalColor("\u001B[1m")

  case object TerminalColorLightGray extends TerminalColor("\u001B[0;37m")

}
