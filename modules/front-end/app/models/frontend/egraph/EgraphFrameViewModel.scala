package models.frontend.egraph

trait EgraphFrameViewModel {
  def cssClass: String
  def cssFrameColumnClasses: String
  def cssStoryColumnClasses: String

  def imageWidthPixels: Int
  def imageHeightPixels: Int
}
