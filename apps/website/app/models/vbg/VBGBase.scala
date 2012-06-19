package models.vbg

import services.db.KeyedCaseClass

abstract class VBGBase extends KeyedCaseClass[Long] {

  def getErrorCode: String

}
