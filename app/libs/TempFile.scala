package libs

import java.io.File

/**
 * Provides temporary files from the project's tmp/ directory.
 */
object TempFile {
  // Statically ensure that the path for temporary files exists.
  val dirPath = new File("tmp/files")
  dirPath.mkdirs()

  /**
   * Provides a [[java.io.File]] of the specified name or path from the temp directory.
   *
   * For example, TempFile.named("herp.txt") would return a file with path
   * $PROJ_BASE/tmp/test-files/herp.txt
   */
  def named(fileName: String): File = {
    new File(dirPath.getAbsolutePath + "/" + fileName)
  }
}