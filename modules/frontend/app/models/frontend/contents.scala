package models.frontend.contents

case class Section(title: String, url: String, subsection: Option[List[Section]]) {

  def renderHtml : String= {
    val head = "<li><div class=\"arrow-right invisible\"></div><a class=\"toc-link\" href=\""+ url +"\">" + title + "</a>"

    val middle  = subsection.map(sub => {
      val contents =
        for(s <- sub) yield {
          s.renderHtml
        }
      "<ul>" + contents.reduceLeft(( (x:String,y:String) => x + y )) + "</ul>"
      }
    )

    head + middle.getOrElse("") + "</li>"
  }


}