package helpers

import models.frontend.footer.FooterData
import models.frontend.header.HeaderData
import models.frontend.header.HeaderNotLoggedIn

/** Provides a controller with a default implicit def for HeaderData */
trait DefaultHeaderAndFooterData {
    implicit val defaultFooterData = {
      FooterData(
        aboutUsLink="about-us",
        faqLink="faq-link",
        termsOfUseLink="terms-of-use",
        privacyPolicyLink="privacy-policy",
        careersPolicyLink="careers",
        egraphsFacebookLink="http://www.facebook.com/egraphs",
        egraphsTwitterLink="http://www.twitter.com/egraphs"
      )
  }

  implicit val defaultHeaderData = {
    HeaderData(
      loggedInStatus=Left(HeaderNotLoggedIn("/login-link")),
      insideAnEgraphLink="inside-an-egraph",
      egraphsFacebookLink="http://www.facebook.com/egraphs",
      enableLogging=true,
      egraphsTwitterLink="http://www.twitter.com/egraphs"
    )
  }
}

object DefaultHeaderAndFooterData extends DefaultHeaderAndFooterData 