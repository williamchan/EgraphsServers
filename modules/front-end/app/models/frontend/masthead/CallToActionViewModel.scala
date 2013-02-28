package models.frontend.masthead

sealed trait CallToActionViewModel {
  def text: String
  def target: String
}

case class SimpleLinkViewModel(text: String, target: String) extends CallToActionViewModel
case class VideoPlayerViewModel(text: String, target: String) extends CallToActionViewModel
case class SearchBoxViewModel(text: String, target: String) extends CallToActionViewModel