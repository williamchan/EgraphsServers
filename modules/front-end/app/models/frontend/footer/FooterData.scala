package models.frontend.footer

/**
 * Data needed to format the footer
 *
 * @param aboutUsLink link to the "about us" page
 * @param faqLink link to the "faq" page
 * @param affiliatesLink link to the "affiliates" page
 * @param termsOfUseLink link to the "terms of use" page
 * @param privacyPolicyLink link to the "privacy policy" page
 * @param careersPolicyLink link to the "careers" page
 * @param egraphsTwitterLink link to the Egraphs twitter page
 * @param egraphsFacebookLink link to the Egraphs facebook page
 */
case class FooterData(
  aboutUsLink: String="/about",
  faqLink: String="/faq",
  affiliatesLink: String="/affiliates",
  termsOfUseLink: String="/terms",
  privacyPolicyLink: String="/privacy",
  blogLink: String= "http://blog.egraphs.com",
  careersPolicyLink: String="/careers",
  egraphsTwitterLink: String = "http://www.twitter.com/egraphs",
  egraphsFacebookLink: String = "http://www.facebook.com/egraphs",
  mailUrl: String = "/Account/subscribe"
)
