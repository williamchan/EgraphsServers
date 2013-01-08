package egraphs.authtoken

import play.api.templates.{Html, HtmlFormat, PlayMagic}
import play.api.libs.json.Json

/**
 * A pair of template helpers that inject authenticity tokens into your forms.
 */
private[authtoken] trait AuthenticityTokenFormHelpers {
  import AuthenticityToken.authTokenKey

  def safeForm(attributes: (Symbol, String)*)(contents: => Html)(implicit token: AuthenticityToken): Html = {
    val attributesHtml = PlayMagic.toHtmlArgs(attributes.toMap)    
    
    Html("<form ") + attributesHtml + Html(">") + hiddenInput + contents + Html("</form>")
  }

  def hiddenInput(implicit token: AuthenticityToken): Html = {
    Html("<input type=\"hidden\" name=\"" + authTokenKey + "\" value=\"" + token.value + "\" />")
  }

  def asJsonObject(implicit token: AuthenticityToken): Html = {
    //TODO: would be better to do in with Json libraries.  This code below has the problem of extra ""s around authTokenKey 
//    Html(Json.stringify(Json.toJson(Map(authTokenKey -> Seq(token.value)))))
    Html("{" + authTokenKey + ": \"" + token.value + "\"}")
  }
}
